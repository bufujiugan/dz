#!/usr/bin/env python3
"""
Deploy DZ Tavern backend services on a Linux server.

What it does:
  1. Installs Git, MySQL and Nginx with the system package manager.
  2. Installs JDK 17 and Maven under /install by default.
  3. Clones or updates https://github.com/bufujiugan/dz.git under /works/dz.
  4. Builds dz-api and dz-admin with Maven.
  5. Creates the dz database and dz_app database user.
  6. Writes /etc/dz/dz.env.
  7. Installs and restarts systemd services: dz-api and dz-admin.
  8. Optionally writes an Nginx reverse proxy config.

Run as root:
  mkdir -p /opt/python-scripts
  cd /opt/python-scripts
  python3 deploy_dz_server.py --configure-nginx --server-name YOUR_SERVER_IP
"""

from __future__ import annotations

import argparse
import getpass
import os
import platform
import secrets
import shutil
import subprocess
import sys
import tarfile
import textwrap
import time
import urllib.request
from pathlib import Path


DEFAULT_REPO = "https://github.com/bufujiugan/dz.git"
DEFAULT_BRANCH = "main"
API_JAR_NAME = "dz-api-1.0.0-SNAPSHOT.jar"
ADMIN_JAR_NAME = "dz-admin-1.0.0-SNAPSHOT.jar"
MAVEN_VERSION = "3.9.9"


class DeployError(RuntimeError):
    pass


def info(message: str) -> None:
    print(f"\n==> {message}", flush=True)


def warn(message: str) -> None:
    print(f"\n[WARN] {message}", flush=True)


def fail(message: str) -> None:
    raise DeployError(message)


def run(
    command: list[str],
    *,
    cwd: Path | None = None,
    env: dict[str, str] | None = None,
    input_text: str | None = None,
    check: bool = True,
    quiet: bool = False,
) -> subprocess.CompletedProcess[str]:
    if not quiet:
        location = f" (cwd={cwd})" if cwd else ""
        print(f"$ {' '.join(command)}{location}", flush=True)
    completed = subprocess.run(
        command,
        cwd=str(cwd) if cwd else None,
        env=env,
        input=input_text,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    if not quiet and completed.stdout:
        print(completed.stdout.rstrip(), flush=True)
    if check and completed.returncode != 0:
        output = completed.stdout.strip()
        raise DeployError(
            f"Command failed with exit code {completed.returncode}: {' '.join(command)}\n{output}"
        )
    return completed


def require_root() -> None:
    if os.name != "posix":
        fail("This script only supports Linux servers.")
    if os.geteuid() != 0:
        fail("Please run this script as root, for example: sudo python3 deploy_dz_server.py")


def command_exists(name: str) -> bool:
    return shutil.which(name) is not None


def detect_package_manager() -> str:
    for manager in ("apt-get", "dnf", "yum"):
        if command_exists(manager):
            return manager
    fail("No supported package manager found. Supported: apt-get, dnf, yum.")
    return ""


def install_packages(args: argparse.Namespace) -> None:
    if args.skip_install:
        info("Skipping OS package installation because --skip-install was provided.")
        return

    manager = detect_package_manager()
    info(f"Installing required OS packages with {manager}")
    if manager == "apt-get":
        env = dict(os.environ)
        env["DEBIAN_FRONTEND"] = "noninteractive"
        run(["apt-get", "update"], env=env)
        run(
            [
                "apt-get",
                "install",
                "-y",
                "git",
                "curl",
                "ca-certificates",
                "nginx",
                "mysql-server",
            ],
            env=env,
        )
        return

    if manager in ("dnf", "yum"):
        base_packages = ["git", "curl", "ca-certificates", "nginx"]
        run([manager, "install", "-y", *base_packages])
        mysql_result = run([manager, "install", "-y", "mysql-server"], check=False)
        if mysql_result.returncode != 0:
            warn("mysql-server package was not available. Trying mariadb-server.")
            run([manager, "install", "-y", "mariadb-server"])
        return


def start_first_existing_service(candidates: list[str], *, required: bool) -> str | None:
    for service in candidates:
        result = run(["systemctl", "list-unit-files", f"{service}.service"], check=False, quiet=True)
        if service in result.stdout:
            run(["systemctl", "enable", "--now", service])
            return service
    if required:
        fail(f"None of these services exists: {', '.join(candidates)}")
    return None


def start_platform_services(skip_mysql: bool) -> str | None:
    info("Starting platform services")
    mysql_service = None
    if not skip_mysql:
        mysql_service = start_first_existing_service(["mysql", "mysqld", "mariadb"], required=True)
    start_first_existing_service(["nginx"], required=False)
    return mysql_service


def detect_linux_arch() -> str:
    machine = platform.machine().lower()
    if machine in ("x86_64", "amd64"):
        return "x64"
    if machine in ("aarch64", "arm64"):
        return "aarch64"
    fail(f"Unsupported CPU architecture for standalone JDK download: {platform.machine()}")
    return ""


def download_file(url: str, target: Path) -> None:
    if target.exists() and target.stat().st_size > 0:
        print(f"Using existing download: {target}", flush=True)
        return
    info(f"Downloading {url}")
    target.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(url, timeout=120) as response:
        with target.open("wb") as output:
            shutil.copyfileobj(response, output)
    print(f"Downloaded {target}", flush=True)


def safe_extract_tar_gz(archive: Path, destination: Path) -> None:
    destination.mkdir(parents=True, exist_ok=True)
    destination_resolved = destination.resolve()
    with tarfile.open(archive, "r:gz") as tar:
        for member in tar.getmembers():
            target = (destination / member.name).resolve()
            if not str(target).startswith(str(destination_resolved)):
                fail(f"Unsafe archive path detected: {member.name}")
        tar.extractall(destination)


def update_symlink(link_path: Path, target_path: Path) -> None:
    if link_path.is_symlink() or link_path.exists():
        if link_path.is_dir() and not link_path.is_symlink():
            warn(f"{link_path} exists and is a directory, not replacing it.")
            return
        link_path.unlink()
    link_path.symlink_to(target_path, target_is_directory=True)


def find_extracted_directory(parent: Path, prefix: str) -> Path:
    candidates = [path for path in parent.iterdir() if path.is_dir() and path.name.startswith(prefix)]
    if not candidates:
        fail(f"No extracted directory starting with {prefix} under {parent}")
    return sorted(candidates, key=lambda path: path.stat().st_mtime, reverse=True)[0]


def install_standalone_java_maven(args: argparse.Namespace) -> None:
    if args.skip_install:
        info("Skipping standalone JDK/Maven installation because --skip-install was provided.")
        args.java_home = Path(os.environ.get("JAVA_HOME", "")) if os.environ.get("JAVA_HOME") else None
        args.java_bin = Path(shutil.which("java") or "/usr/bin/java")
        args.maven_home = None
        args.maven_bin = Path(shutil.which("mvn") or "/usr/bin/mvn")
        return

    if args.use_system_java_maven:
        info("Using system Java and Maven because --use-system-java-maven was provided.")
        if not command_exists("java") or not command_exists("mvn"):
            manager = detect_package_manager()
            if manager == "apt-get":
                env = dict(os.environ)
                env["DEBIAN_FRONTEND"] = "noninteractive"
                run(["apt-get", "install", "-y", "maven", "openjdk-17-jdk"], env=env)
            else:
                run([manager, "install", "-y", "maven", "java-17-openjdk-devel"])
        args.java_home = Path(os.environ.get("JAVA_HOME", "")) if os.environ.get("JAVA_HOME") else None
        args.java_bin = Path(shutil.which("java") or "/usr/bin/java")
        args.maven_home = None
        args.maven_bin = Path(shutil.which("mvn") or "/usr/bin/mvn")
        return

    info(f"Installing standalone JDK 17 and Maven under {args.tools_dir}")
    arch = detect_linux_arch()
    downloads_dir = args.tools_dir / "downloads"
    jdk_extract_dir = args.tools_dir / "jdk-17-extracted"
    maven_extract_dir = args.tools_dir / "maven-extracted"

    jdk_url = (
        "https://api.adoptium.net/v3/binary/latest/17/ga/linux/"
        f"{arch}/jdk/hotspot/normal/eclipse?project=jdk"
    )
    jdk_archive = downloads_dir / f"temurin-jdk17-linux-{arch}.tar.gz"
    download_file(jdk_url, jdk_archive)
    safe_extract_tar_gz(jdk_archive, jdk_extract_dir)
    jdk_home = find_extracted_directory(jdk_extract_dir, "jdk-")
    update_symlink(args.tools_dir / "jdk17", jdk_home)

    maven_url = (
        "https://archive.apache.org/dist/maven/maven-3/"
        f"{MAVEN_VERSION}/binaries/apache-maven-{MAVEN_VERSION}-bin.tar.gz"
    )
    maven_archive = downloads_dir / f"apache-maven-{MAVEN_VERSION}-bin.tar.gz"
    download_file(maven_url, maven_archive)
    safe_extract_tar_gz(maven_archive, maven_extract_dir)
    maven_home = find_extracted_directory(maven_extract_dir, f"apache-maven-{MAVEN_VERSION}")
    update_symlink(args.tools_dir / "maven", maven_home)

    args.java_home = args.tools_dir / "jdk17"
    args.java_bin = args.java_home / "bin" / "java"
    args.maven_home = args.tools_dir / "maven"
    args.maven_bin = args.maven_home / "bin" / "mvn"
    args.maven_repo = args.tools_dir / "maven-repository"
    args.maven_repo.mkdir(parents=True, exist_ok=True)


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def run_mysql(sql: str, mysql_root_password: str | None) -> subprocess.CompletedProcess[str]:
    env = dict(os.environ)
    if mysql_root_password:
        env["MYSQL_PWD"] = mysql_root_password
    return run(["mysql", "-uroot"], env=env, input_text=sql, check=False, quiet=True)


def configure_mysql(args: argparse.Namespace, generated: dict[str, str]) -> None:
    if args.skip_mysql:
        info("Skipping MySQL initialization because --skip-mysql was provided.")
        return

    info("Creating database and database user")
    db_password = args.db_password
    if not db_password:
        if args.non_interactive:
            db_password = secrets.token_urlsafe(24)
            generated["db_password"] = db_password
        else:
            db_password = getpass.getpass(
                "Set password for MySQL user dz_app (blank = generate one): "
            ).strip()
            if not db_password:
                db_password = secrets.token_urlsafe(24)
                generated["db_password"] = db_password

    args.db_password = db_password

    sql = f"""
CREATE DATABASE IF NOT EXISTS `{args.db_name}`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '{args.db_user}'@'localhost' IDENTIFIED BY {sql_string(db_password)};
CREATE USER IF NOT EXISTS '{args.db_user}'@'127.0.0.1' IDENTIFIED BY {sql_string(db_password)};
ALTER USER '{args.db_user}'@'localhost' IDENTIFIED BY {sql_string(db_password)};
ALTER USER '{args.db_user}'@'127.0.0.1' IDENTIFIED BY {sql_string(db_password)};

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON `{args.db_name}`.* TO '{args.db_user}'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON `{args.db_name}`.* TO '{args.db_user}'@'127.0.0.1';

FLUSH PRIVILEGES;
"""

    result = run_mysql(sql, args.mysql_root_password)
    if result.returncode == 0:
        print("MySQL database initialization completed.", flush=True)
        return

    if args.mysql_root_password:
        fail("MySQL initialization failed. Please check --mysql-root-password.")

    if args.non_interactive:
        fail(
            "MySQL root login without password failed. Re-run with --mysql-root-password "
            "or initialize the database manually."
        )

    warn("MySQL root login without password failed.")
    root_password = getpass.getpass("Enter MySQL root password: ").strip()
    result = run_mysql(sql, root_password)
    if result.returncode != 0:
        fail("MySQL initialization failed:\n" + result.stdout.strip())
    args.mysql_root_password = root_password
    print("MySQL database initialization completed.", flush=True)


def ensure_directories(args: argparse.Namespace) -> None:
    info("Creating deployment directories")
    for path in [
        args.base_dir,
        args.tools_dir,
        args.source_dir,
        args.app_dir,
        args.backup_dir,
        args.upload_dir,
        args.config_dir,
        args.log_dir,
        args.maven_repo,
    ]:
        path.mkdir(parents=True, exist_ok=True)


def update_source(args: argparse.Namespace) -> None:
    info("Cloning or updating project source")
    git_dir = args.source_dir / ".git"
    if git_dir.exists():
        run(["git", "remote", "set-url", "origin", args.repo], cwd=args.source_dir)
        run(["git", "fetch", "origin", args.branch], cwd=args.source_dir)
        run(["git", "checkout", args.branch], cwd=args.source_dir)
        run(["git", "pull", "--ff-only", "origin", args.branch], cwd=args.source_dir)
        return

    if args.source_dir.exists() and any(args.source_dir.iterdir()):
        fail(
            f"{args.source_dir} already exists but is not a Git repository. "
            "Move it manually or choose another --base-dir."
        )

    run(["git", "clone", "--branch", args.branch, args.repo, str(args.source_dir)])


def build_project(args: argparse.Namespace) -> None:
    info("Building project with Maven")
    env = dict(os.environ)
    if args.java_home:
        env["JAVA_HOME"] = str(args.java_home)
    if args.maven_home:
        env["MAVEN_HOME"] = str(args.maven_home)
    env["PATH"] = os.pathsep.join(
        [
            str(args.maven_bin.parent),
            str(args.java_bin.parent),
            env.get("PATH", ""),
        ]
    )
    run(
        [
            str(args.maven_bin),
            "clean",
            "package",
            "-DskipTests",
            f"-Dmaven.repo.local={args.maven_repo}",
        ],
        cwd=args.source_dir,
        env=env,
    )


def find_jar(module_dir: Path, jar_name: str) -> Path:
    expected = module_dir / "target" / jar_name
    if expected.exists():
        return expected
    matches = sorted((module_dir / "target").glob("*.jar"))
    if not matches:
        fail(f"No jar file found under {module_dir / 'target'}")
    return matches[0]


def backup_existing(path: Path, backup_dir: Path) -> None:
    if not path.exists():
        return
    timestamp = time.strftime("%Y%m%d-%H%M%S")
    backup_path = backup_dir / f"{path.stem}-{timestamp}{path.suffix}"
    shutil.copy2(path, backup_path)
    print(f"Backed up {path} -> {backup_path}", flush=True)


def install_jars(args: argparse.Namespace) -> None:
    info("Installing built jar files")
    api_jar = find_jar(args.source_dir / "dz-api", API_JAR_NAME)
    admin_jar = find_jar(args.source_dir / "dz-admin", ADMIN_JAR_NAME)
    target_api = args.app_dir / "dz-api.jar"
    target_admin = args.app_dir / "dz-admin.jar"

    backup_existing(target_api, args.backup_dir)
    backup_existing(target_admin, args.backup_dir)

    shutil.copy2(api_jar, target_api)
    shutil.copy2(admin_jar, target_admin)
    print(f"Installed {target_api}", flush=True)
    print(f"Installed {target_admin}", flush=True)


def parse_bool_text(value: str | None) -> bool | None:
    if value is None:
        return None
    normalized = value.strip().lower()
    if normalized in ("1", "true", "yes", "y", "on"):
        return True
    if normalized in ("0", "false", "no", "n", "off"):
        return False
    fail(f"Invalid boolean value: {value}")
    return None


def yes_no_prompt(message: str, default: bool) -> bool:
    suffix = "Y/n" if default else "y/N"
    answer = input(f"{message} [{suffix}]: ").strip().lower()
    if not answer:
        return default
    return answer in ("y", "yes")


def collect_runtime_config(args: argparse.Namespace, generated: dict[str, str]) -> None:
    info("Collecting runtime configuration")

    if not args.jwt_secret:
        args.jwt_secret = secrets.token_hex(32)
        generated["jwt_secret"] = args.jwt_secret

    if not args.admin_password:
        if args.non_interactive:
            args.admin_password = secrets.token_urlsafe(18)
            generated["admin_password"] = args.admin_password
        else:
            value = getpass.getpass(
                "Set initial admin password for username admin (blank = generate one): "
            ).strip()
            if value:
                args.admin_password = value
            else:
                args.admin_password = secrets.token_urlsafe(18)
                generated["admin_password"] = args.admin_password

    if not args.wechat_app_id and not args.non_interactive:
        args.wechat_app_id = input("WECHAT_APP_ID (blank if not ready): ").strip()

    if not args.wechat_app_secret and not args.non_interactive:
        args.wechat_app_secret = getpass.getpass(
            "WECHAT_APP_SECRET (blank if not ready): "
        ).strip()

    mock_value = parse_bool_text(args.wechat_auth_mock)
    if mock_value is None:
        # Keep the service usable when real WeChat credentials are not ready.
        mock_value = not (args.wechat_app_id and args.wechat_app_secret)
    args.wechat_auth_mock_value = "true" if mock_value else "false"


def write_runtime_env(args: argparse.Namespace) -> None:
    info("Writing runtime environment file")
    env_path = args.config_dir / "dz.env"
    backup_existing(env_path, args.backup_dir)

    content = textwrap.dedent(
        f"""\
        DB_URL=jdbc:mysql://127.0.0.1:3306/{args.db_name}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
        DB_USERNAME={args.db_user}
        DB_PASSWORD={args.db_password}
        JWT_SECRET={args.jwt_secret}
        ADMIN_INITIAL_PASSWORD={args.admin_password}
        WECHAT_MOCK_ENABLED=true
        WECHAT_AUTH_MOCK_ENABLED={args.wechat_auth_mock_value}
        WECHAT_APP_ID={args.wechat_app_id or ""}
        WECHAT_APP_SECRET={args.wechat_app_secret or ""}
        WECHAT_MCH_ID={args.wechat_mch_id or ""}
        WECHAT_MERCHANT_PRIVATE_KEY_PATH={args.wechat_merchant_private_key_path or ""}
        WECHAT_MERCHANT_SERIAL_NUMBER={args.wechat_merchant_serial_number or ""}
        WECHAT_API_V3_KEY={args.wechat_api_v3_key or ""}
        UPLOAD_ROOT={args.upload_dir}
        """
    )
    env_path.write_text(content, encoding="utf-8")
    env_path.chmod(0o600)
    print(f"Wrote {env_path}", flush=True)


def service_unit(name: str, jar_path: Path, args: argparse.Namespace) -> str:
    return textwrap.dedent(
        f"""\
        [Unit]
        Description=DZ Tavern {name} Service
        After=network.target mysql.service mysqld.service mariadb.service

        [Service]
        Type=simple
        WorkingDirectory={args.app_dir}
        EnvironmentFile={args.config_dir / "dz.env"}
        ExecStart={args.java_bin} -jar {jar_path}
        Restart=always
        RestartSec=5
        SuccessExitStatus=143
        StandardOutput=append:{args.log_dir / f"{name}.out.log"}
        StandardError=append:{args.log_dir / f"{name}.err.log"}

        [Install]
        WantedBy=multi-user.target
        """
    )


def install_systemd_services(args: argparse.Namespace) -> None:
    info("Installing systemd services")
    services = {
        "dz-api": args.app_dir / "dz-api.jar",
        "dz-admin": args.app_dir / "dz-admin.jar",
    }
    for service_name, jar_path in services.items():
        unit_path = Path("/etc/systemd/system") / f"{service_name}.service"
        backup_existing(unit_path, args.backup_dir)
        unit_path.write_text(service_unit(service_name, jar_path, args), encoding="utf-8")
        print(f"Wrote {unit_path}", flush=True)

    run(["systemctl", "daemon-reload"])
    for service_name in services:
        run(["systemctl", "enable", service_name])
        run(["systemctl", "restart", service_name])
        run(["systemctl", "status", service_name, "--no-pager"], check=False)


def nginx_proxy_headers() -> str:
    return textwrap.dedent(
        """\
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        """
    ).rstrip()


def configure_nginx(args: argparse.Namespace) -> None:
    should_configure = args.configure_nginx
    if not should_configure and not args.non_interactive:
        should_configure = yes_no_prompt("Configure Nginx reverse proxy now?", False)
    if not should_configure:
        info("Skipping Nginx reverse proxy configuration.")
        return

    server_name = args.server_name
    if not server_name:
        if args.non_interactive:
            server_name = "_"
        else:
            server_name = input("Nginx server_name, domain or server IP (blank = _): ").strip() or "_"
    args.server_name = server_name

    info("Writing Nginx reverse proxy configuration")
    nginx_dir = Path("/etc/nginx/conf.d")
    nginx_dir.mkdir(parents=True, exist_ok=True)
    config_path = nginx_dir / "dz.conf"
    backup_existing(config_path, args.backup_dir)
    headers = nginx_proxy_headers()
    config = textwrap.dedent(
        f"""\
        server {{
            listen 80;
            server_name {server_name};

            client_max_body_size 20m;

            location /api/ {{
                proxy_pass http://127.0.0.1:8080;
                {headers}
            }}

            location /uploads/ {{
                proxy_pass http://127.0.0.1:8080;
                {headers}
            }}

            location /admin-api/ {{
                proxy_pass http://127.0.0.1:8081;
                {headers}
            }}

            location /admin/ {{
                proxy_pass http://127.0.0.1:8081;
                {headers}
            }}

            location / {{
                proxy_pass http://127.0.0.1:8081;
                {headers}
            }}
        }}
        """
    )
    config_path.write_text(config, encoding="utf-8")

    test_result = run(["nginx", "-t"], check=False)
    if test_result.returncode != 0:
        fail("Nginx config test failed. Please fix /etc/nginx/conf.d/dz.conf manually.")
    run(["systemctl", "reload", "nginx"])
    print(f"Wrote {config_path}", flush=True)


def verify_services() -> None:
    info("Verifying local service endpoints")
    for service_name in ("dz-api", "dz-admin"):
        run(["systemctl", "is-active", service_name], check=False)

    if command_exists("curl"):
        run(["curl", "-I", "http://127.0.0.1:8081/admin/"], check=False)
        run(["curl", "-I", "http://127.0.0.1:8080/swagger-ui.html"], check=False)


def normalize_paths(args: argparse.Namespace) -> None:
    args.base_dir = Path(args.base_dir)
    args.tools_dir = Path(args.tools_dir)
    args.source_dir = Path(args.source_dir) if args.source_dir else args.base_dir / "dz"
    args.app_dir = Path(args.app_dir) if args.app_dir else args.base_dir / "dz-apps"
    args.backup_dir = Path(args.backup_dir) if args.backup_dir else args.base_dir / "dz-backups"
    args.upload_dir = Path(args.upload_dir) if args.upload_dir else args.base_dir / "dz-uploads"
    args.config_dir = Path(args.config_dir)
    args.log_dir = Path(args.log_dir)
    args.maven_repo = Path(args.maven_repo) if args.maven_repo else args.tools_dir / "maven-repository"
    args.java_home = None
    args.java_bin = Path(shutil.which("java") or "/usr/bin/java")
    args.maven_home = None
    args.maven_bin = Path(shutil.which("mvn") or "/usr/bin/mvn")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Install environment and deploy DZ Tavern backend services."
    )
    parser.add_argument("--repo", default=DEFAULT_REPO, help="Git repository URL.")
    parser.add_argument("--branch", default=DEFAULT_BRANCH, help="Git branch to deploy.")
    parser.add_argument("--base-dir", default="/works", help="Base project directory.")
    parser.add_argument("--source-dir", default="", help="Git working tree. Default: BASE_DIR/dz.")
    parser.add_argument("--app-dir", default="", help="Runtime jar directory. Default: BASE_DIR/dz-apps.")
    parser.add_argument("--backup-dir", default="", help="Backup directory. Default: BASE_DIR/dz-backups.")
    parser.add_argument("--tools-dir", default="/install", help="Standalone tool directory.")
    parser.add_argument(
        "--maven-repo",
        default="",
        help="Maven local repository. Default: TOOLS_DIR/maven-repository.",
    )
    parser.add_argument("--config-dir", default="/etc/dz", help="Directory for dz.env.")
    parser.add_argument("--log-dir", default="/var/log/dz", help="Directory for service logs.")
    parser.add_argument("--upload-dir", default="", help="Upload directory. Default: BASE_DIR/dz-uploads.")

    parser.add_argument("--db-name", default="dz", help="MySQL database name.")
    parser.add_argument("--db-user", default="dz_app", help="MySQL application user.")
    parser.add_argument("--db-password", default="", help="MySQL application user password.")
    parser.add_argument("--mysql-root-password", default="", help="MySQL root password if required.")

    parser.add_argument("--admin-password", default="", help="Initial password for admin username.")
    parser.add_argument("--jwt-secret", default="", help="JWT secret, at least 32 characters.")
    parser.add_argument("--wechat-app-id", default="", help="WeChat app id.")
    parser.add_argument("--wechat-app-secret", default="", help="WeChat app secret.")
    parser.add_argument(
        "--wechat-auth-mock",
        default=None,
        help="true or false. Default: true when WeChat credentials are blank, otherwise false.",
    )
    parser.add_argument("--wechat-mch-id", default="", help="WeChat merchant id.")
    parser.add_argument(
        "--wechat-merchant-private-key-path",
        default="",
        help="WeChat merchant private key path.",
    )
    parser.add_argument(
        "--wechat-merchant-serial-number",
        default="",
        help="WeChat merchant certificate serial number.",
    )
    parser.add_argument("--wechat-api-v3-key", default="", help="WeChat Pay API v3 key.")

    parser.add_argument(
        "--skip-install",
        action="store_true",
        help="Do not install OS packages or standalone JDK/Maven.",
    )
    parser.add_argument(
        "--use-system-java-maven",
        action="store_true",
        help="Install/use system JDK and Maven instead of /install standalone tools.",
    )
    parser.add_argument("--skip-mysql", action="store_true", help="Do not initialize MySQL.")
    parser.add_argument(
        "--configure-nginx",
        action="store_true",
        help="Write /etc/nginx/conf.d/dz.conf reverse proxy config.",
    )
    parser.add_argument("--server-name", default="", help="Nginx server_name, domain or IP.")
    parser.add_argument(
        "--non-interactive",
        action="store_true",
        help="Do not prompt. Missing secrets will be generated.",
    )
    return parser


def print_summary(args: argparse.Namespace, generated: dict[str, str]) -> None:
    public_host = args.server_name if args.server_name and args.server_name != "_" else "SERVER_IP"
    print(
        textwrap.dedent(
            f"""

            Deployment finished.

            Local checks:
              systemctl status dz-api --no-pager
              systemctl status dz-admin --no-pager
              curl -I http://127.0.0.1:8081/admin/

            Service logs:
              journalctl -u dz-api -f
              journalctl -u dz-admin -f
              tail -f {args.log_dir}/dz-api.out.log
              tail -f {args.log_dir}/dz-admin.out.log

            Paths:
              script directory: /opt/python-scripts
              source directory: {args.source_dir}
              runtime jars: {args.app_dir}
              backups: {args.backup_dir}
              uploads: {args.upload_dir}
              tools: {args.tools_dir}
              Maven local repository: {args.maven_repo}

            Admin URL:
              http://{public_host}/admin/

            Admin username:
              admin
            """
        ).rstrip(),
        flush=True,
    )
    if "admin_password" in generated:
        print(f"Generated admin password: {generated['admin_password']}", flush=True)
    else:
        print("Admin password: the value you provided in this script run.", flush=True)

    if "db_password" in generated:
        print("Generated DB password was written to the env file.", flush=True)
    if "jwt_secret" in generated:
        print("Generated JWT secret was written to the env file.", flush=True)
    print(f"Env file: {args.config_dir / 'dz.env'}", flush=True)
    print("Do not share the env file or screenshots containing secrets.", flush=True)


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    normalize_paths(args)
    generated: dict[str, str] = {}

    try:
        require_root()
        install_packages(args)
        ensure_directories(args)
        install_standalone_java_maven(args)
        start_platform_services(args.skip_mysql)
        configure_mysql(args, generated)
        collect_runtime_config(args, generated)
        update_source(args)
        build_project(args)
        install_jars(args)
        write_runtime_env(args)
        install_systemd_services(args)
        configure_nginx(args)
        verify_services()
        print_summary(args, generated)
        return 0
    except DeployError as exc:
        print(f"\nERROR: {exc}", file=sys.stderr, flush=True)
        return 1
    except KeyboardInterrupt:
        print("\nCanceled by user.", file=sys.stderr, flush=True)
        return 130


if __name__ == "__main__":
    raise SystemExit(main())
