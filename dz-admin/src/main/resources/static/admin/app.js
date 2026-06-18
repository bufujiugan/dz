(() => {
    "use strict";

    const TOKEN_KEY = "dz_admin_token_v2";
    const PROFILE_KEY = "dz_admin_profile_v2";
    const STORE_KEY = "dz_admin_global_store_id_v1";
    const pageTitles = {
        stores: ["STORES", "门店管理"],
        dashboard: ["OVERVIEW", "经营总览"],
        users: ["CUSTOMERS", "用户管理"],
        products: ["CATALOG", "商品管理"],
        orders: ["ORDERS", "订单管理"],
        payments: ["PAYMENTS", "支付记录"],
        content: ["STORE CONTENT", "运营内容"],
        coupons: ["COUPONS", "卡券管理"],
        statistics: ["SALES", "销售统计"],
        recharges: ["RECHARGE", "充值管理"],
        storePoints: ["STORE POINTS", "门店积分"],
        accounts: ["ASSETS", "账户流水"],
        operations: ["AUDIT TRAIL", "操作日志"],
        reconcile: ["RECONCILE", "每日对账"]
    };

    const state = {
        token: localStorage.getItem(TOKEN_KEY) || "",
        profile: readProfile(),
        currentView: "dashboard",
        pages: {},
        filters: {},
        stores: [],
        globalStoreId: localStorage.getItem(STORE_KEY) || "",
        storeRecords: [],
        categories: [],
        activities: [],
        couponTemplates: [],
        rechargeTiers: [],
        storePointUsers: [],
        rechargeTab: "orders",
        couponTab: "templates",
        storePointsTab: "users",
        dialogSubmit: null
    };

    const loginPage = document.getElementById("loginPage");
    const appShell = document.getElementById("appShell");
    const dialog = document.getElementById("editorDialog");
    const dialogForm = document.getElementById("dialogForm");

    function readProfile() {
        try {
            return JSON.parse(localStorage.getItem(PROFILE_KEY) || "null");
        } catch (error) {
            return null;
        }
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function toQuery(params) {
        const search = new URLSearchParams();
        Object.entries(params || {}).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== "") {
                search.set(key, value);
            }
        });
        const query = search.toString();
        return query ? `?${query}` : "";
    }

    function money(value) {
        const amount = Number(value || 0) / 100;
        return `¥ ${amount.toFixed(2)}`;
    }

    function fenToYuan(value) {
        return (Number(value || 0) / 100).toFixed(2);
    }

    function yuanToFen(value) {
        const text = String(value ?? "").trim();
        if (!text) {
            return null;
        }
        const amount = Number(text);
        if (!Number.isFinite(amount) || amount < 0) {
            throw new Error("售卖金额格式不正确");
        }
        return Math.round(amount * 100);
    }

    function dateTime(value) {
        if (!value) {
            return "-";
        }
        return String(value).replace("T", " ").slice(0, 19);
    }

    function statusBadge(status) {
        const text = String(status ?? "-");
        const success = ["1", "ACTIVE", "PAID", "SUCCESS", "COMPLETED", "APPROVED", "REFUNDED"];
        const warning = ["CREATED", "PENDING", "WAIT_PAY", "PROCESSING"];
        const danger = ["0", "DISABLED", "CANCELLED", "CLOSED", "FAILED", "REJECTED"];
        let type = "info";
        if (success.includes(text)) type = "success";
        if (warning.includes(text)) type = "warning";
        if (danger.includes(text)) type = "danger";
        const labels = {
            "1": "启用",
            "0": "停用",
            ACTIVE: "正常",
            CREATED: "待支付",
            PAID: "已支付",
            COMPLETED: "已完成",
            CANCELLED: "已取消",
            REFUNDED: "已退款",
            PENDING: "待审核",
            APPROVED: "已通过",
            REJECTED: "已拒绝",
            SUCCESS: "成功",
            FAILED: "失败"
        };
        return `<span class="badge ${type}">${escapeHtml(labels[text] || text)}</span>`;
    }

    function userStatusBadge(status) {
        return status === 0
            ? '<span class="badge success">正常</span>'
            : '<span class="badge danger">已停用</span>';
    }

    function toast(message, type = "success") {
        const node = document.createElement("div");
        node.className = `toast ${type === "error" ? "error" : ""}`;
        node.textContent = message;
        document.getElementById("toastArea").appendChild(node);
        window.setTimeout(() => node.remove(), 3200);
    }

    function setLoading(loading) {
        document.getElementById("loadingMask").classList.toggle("hidden", !loading);
    }

    async function api(path, options = {}) {
        const headers = new Headers(options.headers || {});
        if (state.token && !options.public) {
            headers.set("Authorization", `Bearer ${state.token}`);
        }
        if (options.body !== undefined && !options.rawBody) {
            headers.set("Content-Type", "application/json");
        }

        const response = await fetch(path, {
            method: options.method || "GET",
            headers,
            body: options.body === undefined
                ? undefined
                : options.rawBody
                    ? options.body
                    : JSON.stringify(options.body)
        });

        if (response.status === 401 && !options.public) {
            logout("登录已过期，请重新登录");
            throw new Error("登录已过期");
        }

        const contentType = response.headers.get("content-type") || "";
        if (options.blob) {
            if (!response.ok) throw new Error(`请求失败：HTTP ${response.status}`);
            return response.blob();
        }

        const result = contentType.includes("application/json")
            ? await response.json()
            : await response.text();
        if (!response.ok) {
            throw new Error(result?.msg || result || `请求失败：HTTP ${response.status}`);
        }
        if (typeof result === "object" && result !== null && "code" in result) {
            if (result.code !== 0) {
                throw new Error(result.msg || "业务处理失败");
            }
            return result.data;
        }
        return result;
    }

    function saveSession(data) {
        state.token = data.token;
        state.profile = {
            realName: data.realName || "管理员",
            permissions: Array.from(data.permissions || [])
        };
        localStorage.setItem(TOKEN_KEY, state.token);
        localStorage.setItem(PROFILE_KEY, JSON.stringify(state.profile));
    }

    function logout(message) {
        state.token = "";
        state.profile = null;
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(PROFILE_KEY);
        appShell.classList.add("hidden");
        loginPage.classList.remove("hidden");
        if (message) {
            document.getElementById("loginError").textContent = message;
        }
    }

    function applyProfile() {
        const name = state.profile?.realName || "管理员";
        document.getElementById("adminName").textContent = name;
        document.getElementById("adminAvatar").textContent = name.slice(0, 1);
        const permissions = new Set(state.profile?.permissions || []);
        document.querySelectorAll("[data-permission]").forEach((node) => {
            node.classList.toggle("hidden", !permissions.has(node.dataset.permission));
        });
    }

    function enterApp() {
        loginPage.classList.add("hidden");
        appShell.classList.remove("hidden");
        applyProfile();
        navigate("dashboard");
    }

    function navigate(view) {
        state.currentView = view;
        appShell.dataset.view = view;
        document.querySelectorAll(".view").forEach((node) => node.classList.remove("active"));
        document.getElementById(`view-${view}`).classList.add("active");
        document.querySelectorAll(".nav-item").forEach((node) => {
            node.classList.toggle("active", node.dataset.view === view);
        });
        const [eyebrow, title] = pageTitles[view];
        document.getElementById("pageEyebrow").textContent = eyebrow;
        document.getElementById("pageTitle").textContent = title;
        document.getElementById("sidebar").classList.remove("open");
        loadView(view).catch((error) => {
            toast(error.message || "数据加载失败", "error");
            renderLoadError(view, error.message);
        });
    }

    async function loadView(view) {
        const loaders = {
            dashboard: loadDashboard,
            users: loadUsers,
            products: loadProducts,
            stores: loadStoresManagement,
            orders: loadOrders,
            payments: loadPayments,
            content: loadContent,
            coupons: loadCoupons,
            statistics: loadStatistics,
            recharges: loadRecharges,
            storePoints: loadStorePoints,
            accounts: loadAccounts,
            operations: loadOperations,
            reconcile: loadReconcile
        };
        await loaders[view]();
    }

    function renderLoadError(view, message) {
        if (view === "dashboard") return;
        document.getElementById(`view-${view}`).innerHTML = `
            <div class="empty-state">
                <div><strong>数据加载失败</strong><span>${escapeHtml(message || "请稍后重试")}</span></div>
            </div>`;
    }

    function pageOf(view) {
        return state.pages[view] || 1;
    }

    function setPage(view, page) {
        state.pages[view] = Math.max(1, Number(page) || 1);
        loadView(view).catch((error) => toast(error.message, "error"));
    }

    function pager(view, result) {
        const current = Number(result.current || 1);
        const size = Number(result.size || 10);
        const total = Number(result.total || 0);
        const pages = Math.max(1, Math.ceil(total / size));
        return `
            <div class="pager">
                <span>共 ${total} 条，第 ${current}/${pages} 页</span>
                <button class="table-button" type="button" onclick="adminApp.setPage('${view}', ${current - 1})" ${current <= 1 ? "disabled" : ""}>上一页</button>
                <button class="table-button" type="button" onclick="adminApp.setPage('${view}', ${current + 1})" ${current >= pages ? "disabled" : ""}>下一页</button>
            </div>`;
    }

    function emptyRow(columns, text = "暂无数据") {
        return `<tr><td colspan="${columns}"><div class="empty-state"><div><strong>${text}</strong><span>调整筛选条件后再试试</span></div></div></td></tr>`;
    }

    function settledValue(results, index, fallback) {
        return results[index]?.status === "fulfilled" ? results[index].value : fallback;
    }

    function pageTotal(result) {
        const total = Number(result?.total || 0);
        return Number.isFinite(total) ? total : 0;
    }

    function pageRecords(result) {
        return Array.isArray(result?.records) ? result.records : [];
    }

    function formatCount(value) {
        const number = Number(value || 0);
        return Number.isFinite(number) ? number.toLocaleString("zh-CN") : "-";
    }

    function formatDateLabel(date) {
        return date.toLocaleDateString("zh-CN", {
            year: "numeric",
            month: "long",
            day: "numeric",
            weekday: "short"
        });
    }

    function trendText(current, previous) {
        const currentValue = Number(current || 0);
        const previousValue = Number(previous || 0);
        if (previousValue === 0) {
            return currentValue > 0 ? "较昨日新增" : "较昨日持平";
        }
        const rate = ((currentValue - previousValue) / previousValue * 100).toFixed(1);
        return `较昨日 ${Number(rate) >= 0 ? "+" : ""}${rate}% ${Number(rate) >= 0 ? "↑" : "↓"}`;
    }

    function findDailySales(rows, dateKey) {
        return (rows || []).find((item) => String(item.saleDate).slice(0, 10) === dateKey) || {};
    }

    function setDashboardHtml(id, html) {
        const node = document.getElementById(id);
        if (!node) return;
        node.classList.remove("loading-card");
        node.innerHTML = html;
    }

    function renderDashboardHero(statistics, userTotal) {
        const today = new Date();
        const yesterday = new Date(today);
        yesterday.setDate(today.getDate() - 1);
        const dailySales = Array.isArray(statistics.dailySales) ? statistics.dailySales : [];
        const todayRow = findDailySales(dailySales, formatDateInput(today));
        const yesterdayRow = findDailySales(dailySales, formatDateInput(yesterday));
        const metrics = [
            ["今日营业额", money(todayRow.salesFen || 0), trendText(todayRow.salesFen, yesterdayRow.salesFen)],
            ["今日订单数", formatCount(todayRow.orderCount || 0), trendText(todayRow.orderCount, yesterdayRow.orderCount)],
            ["会员总数", formatCount(userTotal), "当前注册用户"]
        ];
        setDashboardHtml("heroMetricGrid", metrics.map(([label, value, note]) => `
            <article>
                <span>${label}</span>
                <strong>${value}</strong>
                <small>${note}</small>
            </article>`).join(""));
    }

    function renderDashboardSalesChart(statistics) {
        const rows = Array.isArray(statistics.dailySales) ? statistics.dailySales : [];
        if (!rows.length) {
            setDashboardHtml("dashboardSalesChart", `<div class="dashboard-empty"><strong>暂无趋势数据</strong><span>产生订单后会自动展示近 7 天走势。</span></div>`);
            return;
        }
        const maxSales = Math.max(...rows.map((item) => Number(item.salesFen || 0)), 1);
        setDashboardHtml("dashboardSalesChart", rows.map((item) => {
            const height = Math.max(10, Math.round(Number(item.salesFen || 0) / maxSales * 100));
            return `
                <div class="sales-chart__bar" style="--height:${height}%">
                    <span>${money(item.salesFen)}</span>
                    <i></i>
                    <small>${escapeHtml(String(item.saleDate).slice(5))}</small>
                </div>`;
        }).join(""));
    }

    function renderDashboardCouponUsage(templates, categories) {
        const categoryNameById = Object.fromEntries((categories || []).map((item) => [item.id, item.name]));
        const counts = {};
        (templates || []).forEach((item) => {
            const name = item.categoryName || categoryNameById[item.categoryId] || "未分类";
            counts[name] = (counts[name] || 0) + 1;
        });
        const rows = Object.entries(counts).sort((left, right) => right[1] - left[1]);
        const total = (templates || []).length;
        if (!total) {
            setDashboardHtml("dashboardCouponUsage", `<div class="dashboard-empty"><strong>暂无卡券模板</strong><span>创建卡券后这里会展示分类占比。</span></div>`);
            return;
        }
        setDashboardHtml("dashboardCouponUsage", `
            <div class="coupon-donut"><strong>${formatCount(total)}</strong><span>模板总数</span></div>
            <div class="coupon-legend">
                ${rows.slice(0, 5).map(([name, count], index) => `
                    <div>
                        <span><i style="--dot:${index}"></i>${escapeHtml(name)}</span>
                        <strong>${Math.round(count / total * 100)}%</strong>
                        <em>${formatCount(count)}</em>
                    </div>`).join("")}
            </div>`);
    }

    function renderDashboardRecentOrders(orderPage) {
        const records = pageRecords(orderPage).slice(0, 5);
        if (!records.length) {
            setDashboardHtml("dashboardRecentOrders", `<div class="dashboard-empty"><strong>暂无最近订单</strong><span>小程序下单后会同步到这里。</span></div>`);
            return;
        }
        setDashboardHtml("dashboardRecentOrders", records.map((item) => `
            <div class="recent-order-row">
                <div class="recent-order-user">DZ</div>
                <div>
                    <strong>${escapeHtml(item.orderNo)}</strong>
                    <span>用户 ${escapeHtml(item.userId || "-")} · ${escapeHtml(item.nickname || "-")} / ${escapeHtml(item.phone || "-")} · 门店 ${escapeHtml(item.storeId || "-")}</span>
                </div>
                <div class="recent-order-product">${escapeHtml(item.payType || "待选择支付")}</div>
                <div class="money">${money(item.totalFen)}</div>
                <div>${statusBadge(item.status)}</div>
                <time>${dateTime(item.createTime)}</time>
            </div>`).join(""));
    }

    async function loadDashboard() {
        const today = new Date();
        const startDate = new Date(today);
        startDate.setDate(today.getDate() - 6);
        document.getElementById("dashboardGreeting").textContent = `欢迎回来，${state.profile?.realName || "Admin"}`;
        document.getElementById("dashboardDate").textContent = formatDateLabel(today);

        await ensureStores().catch(() => []);
        const selectedStore = currentStore() || { id: 10001, name: "DZ Tavern（滨江店）" };

        const requests = [
            api(`/admin-api/user/page${toQuery({ storeId: selectedStore.id, current: 1, size: 1 })}`),
            api(`/admin-api/order/page${toQuery({ storeId: selectedStore.id, current: 1, size: 5 })}`),
            api(`/admin-api/pay/page${toQuery({ storeId: selectedStore.id, current: 1, size: 1 })}`),
            api(`/admin-api/store-points/requests${toQuery({ storeId: selectedStore.id, status: "PENDING", current: 1, size: 1 })}`),
            api(`/admin-api/statistics/sales${toQuery({
                storeId: selectedStore.id,
                startDate: formatDateInput(startDate),
                endDate: formatDateInput(today)
            })}`),
            api(`/admin-api/coupon/templates${toQuery({ storeId: selectedStore.id })}`),
            api(`/admin-api/coupon/categories${toQuery({ storeId: selectedStore.id })}`),
            api(`/admin-api/recharge/page${toQuery({ storeId: selectedStore.id, current: 1, size: 1 })}`)
        ];
        const results = await Promise.allSettled(requests);
        const userPage = settledValue(results, 0, {});
        const orderPage = settledValue(results, 1, {});
        const payPage = settledValue(results, 2, {});
        const storePointPage = settledValue(results, 3, {});
        const statistics = settledValue(results, 4, { dailySales: [], salesFen: 0, orderCount: 0, itemQuantity: 0 });
        const couponTemplates = settledValue(results, 5, []);
        const couponCategories = settledValue(results, 6, []);
        const rechargePage = settledValue(results, 7, {});
        const userTotal = pageTotal(userPage);
        const pendingStorePoints = pageTotal(storePointPage);

        renderDashboardHero(statistics, userTotal);
        renderDashboardSalesChart(statistics);
        renderDashboardCouponUsage(couponTemplates, couponCategories);
        renderDashboardRecentOrders(orderPage);

        const cards = [
            ["用户总数", formatCount(userTotal), "客", "平台注册用户"],
            ["订单总数", formatCount(pageTotal(orderPage)), "单", "全部业务订单"],
            ["近7天营业额", money(statistics.salesFen || 0), "营", `${formatDateInput(startDate)} 至 ${formatDateInput(today)}`],
            ["卡券模板", formatCount(couponTemplates.length), "券", "可售与可赠卡券"],
            ["待审积分", formatCount(pendingStorePoints), "审", `充值单 ${formatCount(pageTotal(rechargePage))} · 支付记录 ${formatCount(pageTotal(payPage))}`]
        ];
        document.getElementById("statGrid").innerHTML = cards.map(([label, value, symbol, note]) => `
            <article class="stat-card">
                <div class="stat-top"><span>${label}</span><span class="stat-symbol">${symbol}</span></div>
                <strong>${value}</strong><small>${note}</small>
            </article>`).join("");
    }

    async function loadUsers() {
        await ensureStores();
        const filters = state.filters.users || {};
        const storeId = currentStoreId();
        const result = await api(`/admin-api/user/page${toQuery({
            storeId,
            keyword: filters.keyword,
            current: pageOf("users"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td><span class="table-main">#${item.id}</span></td>
                <td><span class="table-main">${escapeHtml(item.nickname || "未设置昵称")}</span><span class="table-sub">${escapeHtml(item.openid || "-")}</span></td>
                <td>${item.storeId || "-"}</td>
                <td>${escapeHtml(item.phone || "-")}</td>
                <td>${userStatusBadge(item.status)}</td>
                <td>${dateTime(item.createTime)}</td>
                <td><div class="row-actions">
                    <button class="table-button primary" onclick="adminApp.userDetail(${item.id})" type="button">详情</button>
                    <button class="table-button ${item.status === 0 ? "danger" : ""}" onclick="adminApp.toggleUser(${item.id}, ${item.status === 0 ? 1 : 0})" type="button">${item.status === 0 ? "停用" : "启用"}</button>
                </div></td>
            </tr>`).join("");
        document.getElementById("view-users").innerHTML = `
            <div class="toolbar product-filter-toolbar">
                <div class="filters">
                    <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || storeId || "-")}" disabled></label>
                    <label class="field wide"><span>关键词</span><input id="userKeyword" value="${escapeHtml(filters.keyword || "")}" placeholder="昵称、手机号或 OpenID"></label>
                    <button class="primary-button" onclick="adminApp.searchUsers()" type="button">查询</button>
                </div>
            </div>
            <div class="table-wrap"><table>
                <thead><tr><th>用户 ID</th><th>用户</th><th>门店</th><th>手机号</th><th>状态</th><th>注册时间</th><th>操作</th></tr></thead>
                <tbody>${rows || emptyRow(7)}</tbody>
            </table></div>${pager("users", result)}`;
    }

    async function userDetail(id) {
        try {
            const item = await api(`/admin-api/user/${id}`);
            openDialog("用户详情", detailGrid([
                ["用户 ID", item.userId],
                ["昵称", item.nickname || "-"],
                ["手机号", item.phone || "-"],
                ["账户余额", money(item.balanceFen)],
                ["可用积分", item.points ?? 0],
                ["冻结积分", item.frozenPoints ?? 0],
                ["状态", item.status === 0 ? "正常" : "停用"]
            ]), null);
        } catch (error) {
            toast(error.message, "error");
        }
    }

    async function toggleUser(id, status) {
        if (!window.confirm(`确认${status === 1 ? "启用" : "停用"}该用户吗？`)) return;
        await runAction(async () => {
            await api(`/admin-api/user/${id}/status?status=${status}`, { method: "POST" });
            toast("用户状态已更新");
            await loadUsers();
        });
    }

    async function loadCatalogOptions() {
        await ensureStores();
        const storeId = currentStoreId();
        state.filters.products = { ...(state.filters.products || {}), storeId };
        if (storeId) {
            state.categories = await api(`/admin-api/catalog/category/list?storeId=${storeId}`);
        }
        return storeId;
    }

    function optionList(items, selected, labelKey = "name", emptyLabel = "全部") {
        return `<option value="">${emptyLabel}</option>` + items.map((item) =>
            `<option value="${item.id}" ${String(item.id) === String(selected || "") ? "selected" : ""}>${escapeHtml(item[labelKey])}</option>`
        ).join("");
    }

    async function loadStoresManagement() {
        const filters = state.filters.stores || {};
        const result = await api(`/admin-api/catalog/store/page${toQuery({
            keyword: filters.keyword,
            status: filters.status,
            current: pageOf("stores"),
            size: 10
        })}`);
        state.storeRecords = result.records || [];
        const rows = state.storeRecords.map((item) => `
            <tr>
                <td><span class="table-main">#${item.id}</span></td>
                <td><span class="table-main">${escapeHtml(item.name)}</span><span class="table-sub">${escapeHtml(item.address || "-")}</span></td>
                <td>${escapeHtml(item.phone || "-")}</td>
                <td>${statusBadge(item.status)}</td>
                <td>${dateTime(item.updateTime || item.createTime)}</td>
                <td><div class="row-actions">
                    <button class="table-button primary" onclick="adminApp.editStore(${item.id})" type="button">编辑</button>
                    <button class="table-button ${item.status === 1 ? "danger" : ""}" onclick="adminApp.toggleStoreStatus(${item.id}, ${item.status === 1 ? 0 : 1})" type="button">${item.status === 1 ? "停用" : "启用"}</button>
                </div></td>
            </tr>`).join("");
        document.getElementById("view-stores").innerHTML = `
            <div class="toolbar product-filter-toolbar">
                <div class="filters">
                    <label class="field wide"><span>门店关键词</span><input id="storeKeyword" value="${escapeHtml(filters.keyword || "")}" placeholder="门店名称、地址或手机号"></label>
                    <label class="field"><span>状态</span><select id="storeStatus">
                        <option value="">全部状态</option>
                        <option value="1" ${String(filters.status || "") === "1" ? "selected" : ""}>启用</option>
                        <option value="0" ${String(filters.status || "") === "0" ? "selected" : ""}>停用</option>
                    </select></label>
                    <button class="primary-button" onclick="adminApp.searchStores()" type="button">查询</button>
                </div>
                <div class="toolbar-actions">
                    <button class="primary-button" onclick="adminApp.editStore()" type="button">新增门店</button>
                </div>
            </div>
            <div class="table-wrap"><table>
                <thead><tr><th>门店 ID</th><th>门店</th><th>联系电话</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
                <tbody>${rows || emptyRow(6, "暂无门店数据")}</tbody>
            </table></div>${pager("stores", result)}`;
    }

    function editStore(id) {
        const store = state.storeRecords.find((item) => Number(item.id) === Number(id)) || {};
        openDialog(id ? "编辑门店" : "新增门店", `
            <div class="form-grid">
                <label class="field full"><span>门店名称</span><input name="name" value="${escapeHtml(store.name || "")}" maxlength="128" required></label>
                <label class="field full"><span>门店地址</span><input name="address" value="${escapeHtml(store.address || "")}" maxlength="255" required></label>
                <label class="field"><span>联系电话</span><input name="phone" value="${escapeHtml(store.phone || "")}" maxlength="32"></label>
                <label class="field"><span>状态</span><select name="status"><option value="1" ${store.status !== 0 ? "selected" : ""}>启用</option><option value="0" ${store.status === 0 ? "selected" : ""}>停用</option></select></label>
            </div>`, async (formData) => {
            await api("/admin-api/catalog/store", {
                method: id ? "PUT" : "POST",
                body: {
                    ...(id ? { id } : {}),
                    name: formData.get("name").trim(),
                    address: formData.get("address").trim(),
                    phone: formData.get("phone").trim(),
                    status: Number(formData.get("status"))
                }
            });
            state.stores = [];
            toast(id ? "门店已更新" : "门店已新增");
            await loadStoresManagement();
        });
    }

    async function toggleStoreStatus(id, status) {
        if (!window.confirm(`确认${status === 1 ? "启用" : "停用"}该门店吗？`)) return;
        await runAction(async () => {
            await api(`/admin-api/catalog/store/${id}/status?status=${status}`, { method: "POST" });
            state.stores = [];
            toast("门店状态已更新");
            await loadStoresManagement();
        });
    }

    function assetUrl(path) {
        if (!path) return "";
        if (/^https?:\/\//i.test(path)) return path;
        return `${window.location.protocol}//${window.location.hostname}:8080${path}`;
    }

    async function uploadProductImage(file) {
        if (!file || !file.size) return "";
        const data = new FormData();
        data.append("file", file);
        const result = await api("/admin-api/common/upload", {
            method: "POST",
            body: data,
            rawBody: true
        });
        return result.url;
    }

    async function loadProducts() {
        const filters = state.filters.products || {};
        state.filters.products = filters;
        const defaultStoreId = await loadCatalogOptions();
        filters.storeId = defaultStoreId;
        const result = await api(`/admin-api/catalog/product/page${toQuery({
            storeId: filters.storeId,
            categoryId: filters.categoryId,
            keyword: filters.keyword,
            current: pageOf("products"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td><span class="table-main">#${item.id}</span></td>
                <td><div class="product-cell">
                    <div class="product-thumb">${item.mainImage
                        ? `<img src="${escapeHtml(assetUrl(item.mainImage))}" alt="">`
                        : "<span>无图</span>"}</div>
                    <div><span class="table-main">${escapeHtml(item.name)}</span><span class="table-sub">门店 ${item.storeId} / 分类 ${item.categoryId}</span></div>
                </div></td>
                <td>${item.recommended === 1 ? statusBadge("ACTIVE").replace("正常", "推荐") : '<span class="badge">普通</span>'}</td>
                <td>${statusBadge(item.status)}</td>
                <td>${dateTime(item.updateTime)}</td>
                <td><div class="row-actions">
                    <button class="table-button primary" onclick="adminApp.editProduct(${item.id})" type="button">编辑</button>
                    <button class="table-button ${item.status === 1 ? "danger" : ""}" onclick="adminApp.toggleProduct(${item.id}, ${item.status === 1 ? 0 : 1})" type="button">${item.status === 1 ? "下架" : "上架"}</button>
                </div></td>
            </tr>`).join("");
        document.getElementById("view-products").innerHTML = `
            <div class="toolbar product-filter-toolbar">
                <div class="filters">
                    <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || filters.storeId || "-")}" disabled></label>
                    <label class="field"><span>分类</span><select id="productCategory">${optionList(state.categories, filters.categoryId)}</select></label>
                    <label class="field wide"><span>商品名称</span><input id="productKeyword" value="${escapeHtml(filters.keyword || "")}" placeholder="输入商品名称"></label>
                    <button class="primary-button" onclick="adminApp.searchProducts()" type="button">查询</button>
                </div>
                <div class="toolbar-actions">
                    <button class="secondary-button" onclick="adminApp.manageCategories()" type="button">分类管理</button>
                    <button class="secondary-button" onclick="adminApp.editAnnouncement()" type="button">首页公告</button>
                    <button class="primary-button" onclick="adminApp.editProduct()" type="button">新增商品</button>
                </div>
            </div>
            <div class="table-wrap"><table>
                <thead><tr><th>商品 ID</th><th>商品</th><th>推荐</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
                <tbody>${rows || emptyRow(6)}</tbody>
            </table></div>${pager("products", result)}`;
    }

    async function editProduct(id) {
        try {
            const detail = id ? await api(`/admin-api/catalog/product/${id}`) : null;
            const product = detail?.product || {};
            const skus = detail?.skus?.length ? detail.skus : [{ specName: "标准", priceFen: 0, stock: 0, sales: 0 }];
            await loadCatalogOptions();
            const editingStoreId = product.storeId || currentStoreId();
            openDialog(id ? "编辑商品" : "新增商品", `
                <div class="form-grid">
                    <input name="storeId" type="hidden" value="${editingStoreId}">
                    <label class="field"><span>当前门店</span><input value="${escapeHtml((state.stores.find((item) => String(item.id) === String(editingStoreId)) || {}).name || editingStoreId || "-")}" disabled></label>
                    <label class="field"><span>分类 ID</span><input name="categoryId" type="number" value="${product.categoryId || state.filters.products?.categoryId || state.categories[0]?.id || ""}" required></label>
                    <label class="field full"><span>商品名称</span><input name="name" value="${escapeHtml(product.name || "")}" maxlength="100" required></label>
                    <div class="field full">
                        <span>商品主图</span>
                        <div class="product-image-editor">
                            <div id="productImagePreview" class="product-image-preview">
                                ${product.mainImage
                                    ? `<img src="${escapeHtml(assetUrl(product.mainImage))}" alt="商品主图">`
                                    : "<span>选择图片后可预览</span>"}
                            </div>
                            <label class="image-upload-button">
                                <input id="productImageFile" name="productImageFile" type="file" accept="image/png,image/jpeg">
                                <strong>选择图片</strong>
                                <small>支持 JPG、PNG，最大 5MB</small>
                            </label>
                        </div>
                        <input name="mainImage" type="hidden" value="${escapeHtml(product.mainImage || "")}">
                    </div>
                    <label class="field full"><span>商品描述</span><textarea name="description">${escapeHtml(product.description || "")}</textarea></label>
                    <label class="field"><span>是否推荐</span><select name="recommended"><option value="0" ${product.recommended !== 1 ? "selected" : ""}>普通</option><option value="1" ${product.recommended === 1 ? "selected" : ""}>推荐</option></select></label>
                    <label class="field"><span>状态</span><select name="status"><option value="1" ${product.status !== 0 ? "selected" : ""}>上架</option><option value="0" ${product.status === 0 ? "selected" : ""}>下架</option></select></label>
                    <label class="field full"><span>SKU JSON</span><textarea name="skus" required>${escapeHtml(JSON.stringify(skus, null, 2))}</textarea><small class="field-note">每项支持 id、specName、priceFen、stock、sales；金额单位为分。</small></label>
                </div>`, async (formData) => {
                let skuList;
                try {
                    skuList = JSON.parse(formData.get("skus"));
                    if (!Array.isArray(skuList)) throw new Error();
                } catch (error) {
                    throw new Error("SKU JSON 格式不正确");
                }
                const imageFile = formData.get("productImageFile");
                let mainImage = formData.get("mainImage");
                if (imageFile && imageFile.size) {
                    mainImage = await uploadProductImage(imageFile);
                }
                if (!mainImage) {
                    throw new Error("请上传商品主图");
                }
                const payload = {
                    product: {
                        ...(id ? { id } : {}),
                        storeId: Number(formData.get("storeId")),
                        categoryId: Number(formData.get("categoryId")),
                        name: formData.get("name"),
                        mainImage,
                        images: JSON.stringify([mainImage]),
                        description: formData.get("description"),
                        recommended: Number(formData.get("recommended")),
                        status: Number(formData.get("status"))
                    },
                    skus: skuList
                };
                await api("/admin-api/catalog/product", { method: id ? "PUT" : "POST", body: payload });
                toast(id ? "商品已更新" : "商品已创建");
                await loadProducts();
            });
            document.getElementById("productImageFile").addEventListener("change", (event) => {
                const file = event.target.files?.[0];
                if (!file) return;
                if (file.size > 5 * 1024 * 1024) {
                    event.target.value = "";
                    toast("图片不能超过 5MB", "error");
                    return;
                }
                document.getElementById("productImagePreview").innerHTML =
                    `<img src="${URL.createObjectURL(file)}" alt="商品主图预览">`;
            });
        } catch (error) {
            toast(error.message, "error");
        }
    }

    async function toggleProduct(id, status) {
        if (!window.confirm(`确认${status === 1 ? "上架" : "下架"}该商品吗？`)) return;
        await runAction(async () => {
            await api(`/admin-api/catalog/product/${id}/status?status=${status}`, { method: "POST" });
            toast("商品状态已更新");
            await loadProducts();
        });
    }

    async function manageCategories() {
        const storeId = currentStoreId();
        if (!storeId) {
            toast("请先选择门店", "error");
            return;
        }
        const categories = await api(`/admin-api/catalog/category/list?storeId=${storeId}`);
        state.categories = categories;
        openDialog("分类管理", `
            <div class="form-grid">
                <input name="storeId" type="hidden" value="${storeId}">
                <label class="field"><span>分类名称</span><input name="name" maxlength="50" required></label>
                <label class="field"><span>排序</span><input name="sort" type="number" value="0" required></label>
            </div>
            <div class="table-wrap" style="margin-top:18px"><table>
                <thead><tr><th>ID</th><th>名称</th><th>排序</th><th>操作</th></tr></thead>
                <tbody>${categories.map((item) => `<tr><td>${item.id}</td><td>${escapeHtml(item.name)}</td><td>${item.sort}</td><td><div class="row-actions"><button type="button" class="table-button" onclick="adminApp.renameCategory(${item.id})">编辑</button><button type="button" class="table-button danger" onclick="adminApp.deleteCategory(${item.id})">删除</button></div></td></tr>`).join("") || emptyRow(4)}</tbody>
            </table></div>`, async (formData) => {
            await api("/admin-api/catalog/category", {
                method: "POST",
                body: {
                    storeId: Number(formData.get("storeId")),
                    name: formData.get("name"),
                    sort: Number(formData.get("sort"))
                }
            });
            toast("分类已新增");
            state.categories = [];
            await loadProducts();
        }, "新增分类");
    }

    async function renameCategory(id) {
        const category = state.categories.find((item) => item.id === id);
        if (!category) return;
        const name = window.prompt("请输入分类名称", category.name);
        if (!name) return;
        const sort = window.prompt("请输入排序值", String(category.sort ?? 0));
        if (sort === null) return;
        await runAction(async () => {
            await api("/admin-api/catalog/category", {
                method: "PUT",
                body: { id, storeId: category.storeId, name, sort: Number(sort) }
            });
            dialog.close();
            toast("分类已更新");
            await loadProducts();
        });
    }

    async function deleteCategory(id) {
        if (!window.confirm("确认删除该分类吗？请先确保分类下没有在用商品。")) return;
        await runAction(async () => {
            await api(`/admin-api/catalog/category/${id}`, { method: "DELETE" });
            dialog.close();
            toast("分类已删除");
            await loadProducts();
        });
    }

    function editAnnouncement() {
        const storeId = currentStoreId();
        if (!storeId) {
            toast("请先选择门店", "error");
            return;
        }
        openDialog("更新首页公告", `
            <input name="storeId" type="hidden" value="${storeId}">
            <label class="field"><span>公告内容</span><textarea name="announcement" maxlength="500" placeholder="输入展示在用户首页的公告"></textarea><small class="field-note">当前门店 ID：${storeId}</small></label>
        `, async (formData) => {
            await api(`/admin-api/catalog/home/announcement?storeId=${storeId}`, {
                method: "PUT",
                rawBody: true,
                headers: { "Content-Type": "text/plain;charset=UTF-8" },
                body: formData.get("announcement")
            });
            toast("首页公告已更新");
        });
    }

    async function loadOrders() {
        await ensureStores();
        const filters = state.filters.orders || {};
        filters.storeId = currentStoreId();
        const result = await api(`/admin-api/order/page${toQuery({
            storeId: filters.storeId,
            orderNo: filters.orderNo,
            status: filters.status,
            current: pageOf("orders"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td><span class="table-main">${escapeHtml(item.orderNo)}</span></td>
                <td><span class="table-main">#${item.userId}</span><span class="table-sub">${escapeHtml(item.nickname || "-")} / ${escapeHtml(item.phone || "-")}</span></td>
                <td>${item.storeId}</td>
                <td class="money">${money(item.totalFen)}</td>
                <td>${escapeHtml(item.payType || "-")}</td>
                <td>${statusBadge(item.status)}</td>
                <td>${dateTime(item.createTime)}</td>
                <td><div class="row-actions">
                    <button class="table-button primary" onclick="adminApp.orderDetail('${escapeHtml(item.orderNo)}')" type="button">详情</button>
                    ${item.status === "PAID" ? `<button class="table-button" onclick="adminApp.completeOrder('${escapeHtml(item.orderNo)}')" type="button">完成</button>` : ""}
                </div></td>
            </tr>`).join("");
        document.getElementById("view-orders").innerHTML = `
            ${filterToolbar(`
                <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || filters.storeId || "-")}" disabled></label>
                <label class="field wide"><span>订单号</span><input id="orderNo" value="${escapeHtml(filters.orderNo || "")}" placeholder="输入订单号"></label>
                <label class="field"><span>状态</span><select id="orderStatus">${statusOptions(filters.status, ["CREATED", "PAID", "COMPLETED", "CANCELLED", "REFUNDED"])}</select></label>
                <button class="primary-button" onclick="adminApp.searchOrders()" type="button">查询</button>`)}
            <div class="table-wrap"><table>
                <thead><tr><th>订单</th><th>用户</th><th>门店</th><th>金额</th><th>支付方式</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
                <tbody>${rows || emptyRow(8)}</tbody>
            </table></div>${pager("orders", result)}`;
    }

    async function orderDetail(orderNo) {
        try {
            const detail = await api(`/admin-api/order/${encodeURIComponent(orderNo)}`);
            openDialog("订单详情", `
                ${detailGrid([
                    ["订单号", detail.order.orderNo],
                    ["用户 ID", detail.order.userId],
                    ["门店 ID", detail.order.storeId],
                    ["订单金额", money(detail.order.totalFen)],
                    ["支付方式", detail.order.payType || "-"],
                    ["状态", detail.order.status],
                    ["下单时间", dateTime(detail.order.createTime)],
                    ["备注", detail.order.remark || "-"]
                ])}
                <h4>商品明细</h4>
                <pre class="json-view">${escapeHtml(JSON.stringify(detail.items, null, 2))}</pre>`, null);
        } catch (error) {
            toast(error.message, "error");
        }
    }

    async function completeOrder(orderNo) {
        if (!window.confirm("确认将该订单标记为已完成吗？")) return;
        await runAction(async () => {
            await api(`/admin-api/order/${encodeURIComponent(orderNo)}/complete`, { method: "POST" });
            toast("订单已完成");
            await loadOrders();
        });
    }

    async function loadPayments() {
        await ensureStores();
        const filters = state.filters.payments || {};
        filters.storeId = currentStoreId();
        const result = await api(`/admin-api/pay/page${toQuery({
            storeId: filters.storeId,
            orderNo: filters.orderNo,
            tradeState: filters.tradeState,
            current: pageOf("payments"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td><span class="table-main">${escapeHtml(item.orderNo)}</span><span class="table-sub">${escapeHtml(item.transactionId || "无交易号")}</span></td>
                <td><span class="table-main">#${item.userId || "-"}</span><span class="table-sub">${escapeHtml(item.nickname || "-")} / ${escapeHtml(item.phone || "-")}</span></td>
                <td>${item.storeId || "-"}</td>
                <td class="money">${money(item.amountFen)}</td>
                <td>${statusBadge(item.tradeState)}</td>
                <td>${escapeHtml(item.verifyResult || "-")}</td>
                <td>${item.notifyCount ?? 0}</td>
                <td>${dateTime(item.createTime)}</td>
                <td><button class="table-button primary" onclick="adminApp.paymentDetail('${escapeHtml(item.orderNo)}')" type="button">详情</button></td>
            </tr>`).join("");
        document.getElementById("view-payments").innerHTML = `
            ${filterToolbar(`
                <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || filters.storeId || "-")}" disabled></label>
                <label class="field wide"><span>订单号</span><input id="paymentOrderNo" value="${escapeHtml(filters.orderNo || "")}" placeholder="输入订单号"></label>
                <label class="field"><span>交易状态</span><input id="paymentState" value="${escapeHtml(filters.tradeState || "")}" placeholder="例如 SUCCESS"></label>
                <button class="primary-button" onclick="adminApp.searchPayments()" type="button">查询</button>`)}
            <div class="table-wrap"><table>
                <thead><tr><th>订单 / 交易号</th><th>用户</th><th>门店</th><th>金额</th><th>交易状态</th><th>验签结果</th><th>通知次数</th><th>创建时间</th><th>操作</th></tr></thead>
                <tbody>${rows || emptyRow(9)}</tbody>
            </table></div>${pager("payments", result)}`;
    }

    async function paymentDetail(orderNo) {
        try {
            const detail = await api(`/admin-api/pay/${encodeURIComponent(orderNo)}`);
            openDialog("支付记录详情", `<pre class="json-view">${escapeHtml(JSON.stringify(detail, null, 2))}</pre>`, null);
        } catch (error) {
            toast(error.message, "error");
        }
    }

    async function ensureStores() {
        if (!state.stores.length) {
            state.stores = await api("/admin-api/catalog/store/list");
        }
        ensureGlobalStore();
        renderGlobalStoreSelect();
        return state.stores;
    }

    function ensureGlobalStore() {
        if (!state.stores.length) {
            state.globalStoreId = "";
            localStorage.removeItem(STORE_KEY);
            return "";
        }
        const matched = state.stores.find((item) => String(item.id) === String(state.globalStoreId));
        if (!matched) {
            state.globalStoreId = String(state.stores[0].id);
            localStorage.setItem(STORE_KEY, state.globalStoreId);
        }
        return state.globalStoreId;
    }

    function currentStoreId() {
        return ensureGlobalStore();
    }

    function currentStore() {
        const storeId = currentStoreId();
        return state.stores.find((item) => String(item.id) === String(storeId)) || null;
    }

    function renderGlobalStoreSelect() {
        const options = state.stores.map((item) =>
            `<option value="${item.id}" ${String(item.id) === String(state.globalStoreId) ? "selected" : ""}>${escapeHtml(item.name)}</option>`
        ).join("");
        [
            ["globalStoreField", "globalStoreSelect"],
            ["dashboardStoreField", "dashboardStoreSelect"]
        ].forEach(([fieldId, selectId]) => {
            const field = document.getElementById(fieldId);
            const select = document.getElementById(selectId);
            if (!field || !select) return;
            field.classList.toggle("hidden", !state.stores.length);
            select.innerHTML = options;
            select.value = state.globalStoreId;
        });
    }

    function resetStoreScopedFilters() {
        [
            "users", "products", "orders", "payments", "content", "coupons",
            "couponIssued", "couponRedeem", "statistics", "storePoints",
            "storePointUsers", "storePointsLogs", "recharges", "points", "accounts"
        ].forEach((key) => {
            if (state.filters[key]) {
                delete state.filters[key].storeId;
            }
        });
        state.pages = {};
    }

    async function changeGlobalStore(storeId) {
        if (!storeId || String(storeId) === String(state.globalStoreId)) return;
        state.globalStoreId = String(storeId);
        localStorage.setItem(STORE_KEY, state.globalStoreId);
        resetStoreScopedFilters();
        renderGlobalStoreSelect();
        await loadView(state.currentView);
        toast("已切换全局门店");
    }

    function storeSelectOptions(selected) {
        return state.stores.map((item) =>
            `<option value="${item.id}" ${String(item.id) === String(selected) ? "selected" : ""}>${escapeHtml(item.name)}</option>`
        ).join("");
    }

    async function loadContent() {
        await ensureStores();
        const storeId = Number(currentStoreId() || 0);
        if (!storeId) {
            document.getElementById("view-content").innerHTML = '<div class="empty-state"><div><strong>暂无门店</strong><span>请先创建门店数据</span></div></div>';
            return;
        }
        state.filters.content = { storeId };
        const [config, activities] = await Promise.all([
            api(`/admin-api/content/store-config?storeId=${storeId}`),
            api(`/admin-api/content/activities?storeId=${storeId}`)
        ]);
        state.activities = activities;
        const hero = assetUrl(config.heroImage);
        const rows = activities.map((item) => `
            <tr>
                <td><div class="product-cell">
                    <span class="product-thumb">${item.imageUrl ? `<img src="${escapeHtml(assetUrl(item.imageUrl))}" alt="">` : "无图"}</span>
                    <span><span class="table-main">${escapeHtml(item.title)}</span><span class="table-sub">活动 #${item.id}</span></span>
                </div></td>
                <td>${item.sort ?? 0}</td>
                <td>${statusBadge(item.status)}</td>
                <td>${dateTime(item.updateTime || item.createTime)}</td>
                <td><div class="row-actions">
                    <button class="table-button primary" onclick="adminApp.editActivity(${item.id})" type="button">编辑</button>
                    <button class="table-button ${item.status === 1 ? "danger" : ""}" onclick="adminApp.toggleActivity(${item.id}, ${item.status === 1 ? 0 : 1})" type="button">${item.status === 1 ? "停用" : "启用"}</button>
                </div></td>
            </tr>`).join("");
        document.getElementById("view-content").innerHTML = `
            <div class="toolbar product-filter-toolbar">
                <div class="filters">
                    <label class="field wide"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || storeId || "-")}" disabled></label>
                </div>
                <div class="toolbar-actions"><button class="primary-button" onclick="adminApp.editActivity()" type="button">新增活动</button></div>
            </div>
            <div class="operation-grid">
                <article class="panel operation-config">
                    <div class="panel-head"><div><p class="panel-kicker">MINI PROGRAM</p><h3>小程序运营配置</h3></div><span class="badge success">门店独立</span></div>
                    <div class="form-grid">
                        <label class="field"><span>今晚营业至</span><input id="contentBusinessEndTime" value="${escapeHtml(config.businessEndTime || "02:00")}" placeholder="02:00"></label>
                        <label class="field"><span>点单页标题</span><input id="contentMenuTitle" value="${escapeHtml(config.menuTitle || "今晚酒单")}" maxlength="64"></label>
                        <label class="field full"><span>首页主标语</span><input id="contentHomeSlogan" value="${escapeHtml(config.homeSlogan || "")}" maxlength="128"></label>
                        <label class="field full"><span>玩法说明</span><textarea id="contentGameplay" maxlength="1000">${escapeHtml(config.gameplayDescription || "")}</textarea></label>
                        <label class="field"><span>积分减半</span><select id="contentHalvingEnabled"><option value="0" ${config.pointsHalvingEnabled !== 1 ? "selected" : ""}>关闭</option><option value="1" ${config.pointsHalvingEnabled === 1 ? "selected" : ""}>启用</option></select></label>
                        <label class="field"><span>间隔自然日</span><input id="contentHalvingDay" type="number" min="1" step="1" value="${escapeHtml(config.pointsHalvingDay || 7)}"></label>
                        <label class="field"><span>执行时间</span><input id="contentHalvingTime" type="time" value="${escapeHtml(config.pointsHalvingTime || "00:00")}"></label>
                        <div class="halving-warning field full"><strong>积分减半规则</strong><span>后台可配置任意间隔天数；到期后按自然日周期自动减半，结果四舍五入。</span></div>
                        <label class="field full"><span>首页主视觉图片</span>
                            <div class="managed-image-row">
                                <div id="contentHeroPreview" class="managed-image-preview">${hero ? `<img src="${escapeHtml(hero)}" alt="">` : "等待上传"}</div>
                                <div class="managed-image-controls">
                                    <input id="contentHeroImage" value="${escapeHtml(config.heroImage || "")}" placeholder="图片地址">
                                    <label class="compact-upload">上传图片<input type="file" accept="image/*" onchange="adminApp.previewManagedImage(this, 'contentHeroImage', 'contentHeroPreview')"></label>
                                </div>
                            </div>
                        </label>
                    </div>
                    <div class="panel-actions"><button class="primary-button" onclick="adminApp.saveStoreConfig()" type="button">保存门店配置</button></div>
                </article>
                <article class="panel">
                    <div class="panel-head"><div><p class="panel-kicker">ACTIVITY FEED</p><h3>活动图片</h3></div><span class="panel-note">按排序值升序展示</span></div>
                    <div class="table-wrap"><table>
                        <thead><tr><th>活动</th><th>排序</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
                        <tbody>${rows || emptyRow(5, "暂无活动图片")}</tbody>
                    </table></div>
                </article>
            </div>`;
    }

    async function previewManagedImage(input, urlInputId, previewId) {
        const file = input.files?.[0];
        if (!file) return;
        await runAction(async () => {
            const url = await uploadProductImage(file);
            document.getElementById(urlInputId).value = url;
            document.getElementById(previewId).innerHTML = `<img src="${escapeHtml(assetUrl(url))}" alt="">`;
            toast("图片上传成功");
        });
    }

    async function saveStoreConfig() {
        const storeId = Number(state.filters.content?.storeId);
        await runAction(async () => {
            await api("/admin-api/content/store-config", {
                method: "PUT",
                body: {
                    storeId,
                    businessEndTime: document.getElementById("contentBusinessEndTime").value.trim(),
                    homeSlogan: document.getElementById("contentHomeSlogan").value.trim(),
                    heroImage: document.getElementById("contentHeroImage").value.trim(),
                    gameplayDescription: document.getElementById("contentGameplay").value.trim(),
                    menuTitle: document.getElementById("contentMenuTitle").value.trim(),
                    pointsHalvingEnabled: Number(document.getElementById("contentHalvingEnabled").value),
                    pointsHalvingDay: Number(document.getElementById("contentHalvingDay").value),
                    pointsHalvingTime: document.getElementById("contentHalvingTime").value
                }
            });
            toast("门店运营配置已保存");
            await loadContent();
        });
    }

    function editActivity(id) {
        const activity = state.activities.find((item) => item.id === id) || {};
        const storeId = Number(state.filters.content?.storeId);
        openDialog(id ? "编辑活动" : "新增活动", `
            <div class="form-grid">
                <label class="field full"><span>活动标题</span><input name="title" value="${escapeHtml(activity.title || "")}" maxlength="128" required></label>
                <label class="field"><span>排序</span><input name="sort" type="number" value="${activity.sort ?? 0}" required></label>
                <label class="field"><span>状态</span><select name="status"><option value="1" ${activity.status !== 0 ? "selected" : ""}>启用</option><option value="0" ${activity.status === 0 ? "selected" : ""}>停用</option></select></label>
                <label class="field full"><span>活动图片</span>
                    <div class="managed-image-row">
                        <div id="activityImagePreview" class="managed-image-preview">${activity.imageUrl ? `<img src="${escapeHtml(assetUrl(activity.imageUrl))}" alt="">` : "等待上传"}</div>
                        <div class="managed-image-controls">
                            <input id="activityImageUrl" name="imageUrl" value="${escapeHtml(activity.imageUrl || "")}" placeholder="图片地址" required>
                            <label class="compact-upload">上传图片<input type="file" accept="image/*" onchange="adminApp.previewManagedImage(this, 'activityImageUrl', 'activityImagePreview')"></label>
                        </div>
                    </div>
                </label>
            </div>`, async (formData) => {
            await api("/admin-api/content/activity", {
                method: id ? "PUT" : "POST",
                body: {
                    ...(id ? { id } : {}),
                    storeId,
                    title: formData.get("title").trim(),
                    imageUrl: formData.get("imageUrl").trim(),
                    sort: Number(formData.get("sort")),
                    status: Number(formData.get("status"))
                }
            });
            toast(id ? "活动已更新" : "活动已创建");
            await loadContent();
        });
    }

    async function toggleActivity(id, status) {
        await runAction(async () => {
            await api(`/admin-api/content/activity/status?id=${id}&status=${status}`, { method: "POST" });
            toast("活动状态已更新");
            await loadContent();
        });
    }

    function couponTabs(active) {
        return `<section class="coupon-admin-hero">
            <div>
                <p>COUPON CENTER</p>
                <h3>卡券运营中心</h3>
                <span>统一管理卡券商品、商家赠送和到店核销。</span>
            </div>
            <div class="coupon-admin-hero__mark">券</div>
        </section>
        <div class="section-tabs coupon-admin-tabs">
            <button class="${active === "templates" ? "active" : ""}" onclick="adminApp.switchCouponTab('templates')" type="button">卡券模板</button>
            <button class="${active === "issued" ? "active" : ""}" onclick="adminApp.switchCouponTab('issued')" type="button">用户卡券</button>
            <button class="${active === "redeem" ? "active" : ""}" onclick="adminApp.switchCouponTab('redeem')" type="button">核销审核</button>
        </div>`;
    }

    async function loadCoupons() {
        await ensureStores();
        if (state.couponTab === "issued") {
            await loadIssuedCoupons();
            return;
        }
        if (state.couponTab === "redeem") {
            await loadCouponRedeems();
            return;
        }
        const storeId = Number(currentStoreId() || 0);
        state.filters.coupons = { ...(state.filters.coupons || {}), storeId };
        const [templates, categories] = storeId
            ? await Promise.all([
                api(`/admin-api/coupon/templates?storeId=${storeId}`),
                api(`/admin-api/coupon/categories?storeId=${storeId}`)
            ])
            : [[], []];
        const decoratedTemplates = await decorateCouponTemplates(templates);
        state.couponTemplates = decoratedTemplates;
        state.couponCategories = categories;
        const categoryNameById = Object.fromEntries(categories.map((item) => [item.id, item.name]));
        const cards = decoratedTemplates.map((item) => `
            <article class="coupon-template-card">
                <div class="coupon-template-card__visual">
                    ${item.imageUrl ? `<img src="${escapeHtml(assetUrl(item.imageUrl))}" alt="">` : `<span>COUPON</span>`}
                    <div class="coupon-template-card__shade"></div>
                    <strong>DZ TAVERN</strong>
                </div>
                <div class="coupon-template-card__body">
                    <div class="coupon-template-card__head">
                        <span class="coupon-template-card__tag">${escapeHtml(item.categoryName || categoryNameById[item.categoryId] || "未分类")}</span>
                        ${statusBadge(item.status)}
                    </div>
                    <h4>${escapeHtml(item.name)}</h4>
                    <p>${escapeHtml(item.description || "暂无说明")}</p>
                    <dl>
                        <div><dt>关联商品</dt><dd>${item.saleProductId || "-"}</dd></div>
                        <div><dt>售卖金额</dt><dd>${item.salePriceFen != null ? money(item.salePriceFen) : "-"}</dd></div>
                        <div><dt>库存</dt><dd>${item.saleStock != null ? item.saleStock : "-"}</dd></div>
                        <div><dt>购买有效期</dt><dd>${item.purchaseValidDays} 天</dd></div>
                        <div><dt>发放方式</dt><dd>${item.saleProductId ? "购买发券" : "商家赠送"}</dd></div>
                    </dl>
                    <div class="row-actions coupon-template-card__actions">
                        <button class="table-button primary" onclick="adminApp.editCouponTemplate(${item.id})" type="button">编辑</button>
                        <button class="table-button" onclick="adminApp.giftCoupon(${item.id})" type="button">赠送</button>
                        <button class="table-button ${item.status === 1 ? "danger" : ""}" onclick="adminApp.toggleCouponTemplate(${item.id}, ${item.status === 1 ? 0 : 1})" type="button">${item.status === 1 ? "停用" : "启用"}</button>
                    </div>
                </div>
            </article>`).join("");
        document.getElementById("view-coupons").innerHTML = `
            ${couponTabs("templates")}
            <div class="toolbar product-filter-toolbar">
                <div class="filters"><label class="field wide"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || storeId || "-")}" disabled></label></div>
                <div class="toolbar-actions"><button class="primary-button" onclick="adminApp.editCouponTemplate()" type="button">新增卡券模板</button></div>
            </div>
            <div class="coupon-admin-guide"><strong>购买与赠送规则</strong><span>模板关联在售商品后只展示在小程序卡券中心，不进入自助点酒；用户完成支付自动发券，管理员也可按用户 ID 赠送。</span></div>
            <div class="coupon-template-grid">${cards || `<div class="empty-state"><strong>暂无卡券模板</strong><span>创建模板后即可在小程序卡券中心呈现。</span></div>`}</div>`;
    }

    async function decorateCouponTemplates(templates) {
        return Promise.all((templates || []).map(async (template) => {
            if (!template.saleProductId) {
                return template;
            }
            try {
                const detail = await api(`/admin-api/catalog/product/${template.saleProductId}`);
                const sku = detail?.skus?.[0] || null;
                return {
                    ...template,
                    saleProductName: detail?.product?.name || "",
                    saleSkuId: sku?.id || null,
                    salePriceFen: sku?.priceFen ?? null,
                    saleStock: sku?.stock ?? null
                };
            } catch (error) {
                return {
                    ...template,
                    saleProductName: "",
                    saleSkuId: null,
                    salePriceFen: null,
                    saleStock: null
                };
            }
        }));
    }

    async function loadIssuedCoupons() {
        await ensureStores();
        const filters = state.filters.couponIssued || {};
        const storeId = currentStoreId();
        state.filters.couponIssued = { ...filters, storeId };
        const result = await api(`/admin-api/coupon/user-page${toQuery({
            storeId,
            userId: filters.userId,
            status: filters.status,
            current: pageOf("coupons"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td><span class="table-main">${escapeHtml(item.couponNo)}</span><span class="table-sub">${escapeHtml(item.couponName)}</span></td>
                <td><span class="table-main">#${item.userId}</span><span class="table-sub">${escapeHtml(item.nickname || "-")} / ${escapeHtml(item.phone || "-")}</span></td><td>${item.storeId}</td>
                <td>${escapeHtml(item.sourceType)}</td><td>${statusBadge(item.status)}</td>
                <td>${dateTime(item.expireTime)}</td>
            </tr>`).join("");
        document.getElementById("view-coupons").innerHTML = `
            ${couponTabs("issued")}
            ${filterToolbar(`
                <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || storeId || "-")}" disabled></label>
                <label class="field"><span>用户 ID</span><input id="couponIssuedUserId" type="number" value="${escapeHtml(filters.userId || "")}"></label>
                <label class="field"><span>状态</span><select id="couponIssuedStatus">${statusOptions(filters.status, ["UNUSED", "REDEEM_PENDING", "USED", "EXPIRED"])}</select></label>
                <button class="primary-button" onclick="adminApp.searchIssuedCoupons()" type="button">查询</button>`)}
            <div class="table-wrap"><table>
                <thead><tr><th>券号 / 名称</th><th>用户</th><th>门店</th><th>来源</th><th>状态</th><th>有效期</th></tr></thead>
                <tbody>${rows || emptyRow(6)}</tbody>
            </table></div>${pager("coupons", result)}`;
    }

    async function loadCouponRedeems() {
        await ensureStores();
        const filters = state.filters.couponRedeem || {};
        const storeId = currentStoreId();
        state.filters.couponRedeem = { ...filters, storeId };
        const result = await api(`/admin-api/coupon/redeem-page${toQuery({
            storeId,
            status: filters.status,
            current: pageOf("coupons"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td>#${item.id}</td><td><span class="table-main">#${item.userCouponId}</span><span class="table-sub">${escapeHtml(item.couponName || item.couponNo || "-")}</span></td><td><span class="table-main">#${item.userId}</span><span class="table-sub">${escapeHtml(item.nickname || "-")} / ${escapeHtml(item.phone || "-")}</span></td><td>${item.storeId || "-"}</td>
                <td>${escapeHtml(item.remark || "-")}</td><td>${statusBadge(item.status)}</td><td>${dateTime(item.createTime)}</td>
                <td>${item.status === "PENDING" ? `<div class="row-actions">
                    <button class="table-button primary" onclick="adminApp.auditCouponRedeem(${item.id}, true)" type="button">通过</button>
                    <button class="table-button danger" onclick="adminApp.auditCouponRedeem(${item.id}, false)" type="button">拒绝</button>
                </div>` : escapeHtml(item.auditRemark || "-")}</td>
            </tr>`).join("");
        document.getElementById("view-coupons").innerHTML = `
            ${couponTabs("redeem")}
            ${filterToolbar(`
                <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || storeId || "-")}" disabled></label>
                <label class="field"><span>状态</span><select id="couponRedeemStatus">${statusOptions(filters.status, ["PENDING", "APPROVED", "REJECTED"])}</select></label>
                <button class="primary-button" onclick="adminApp.searchCouponRedeems()" type="button">查询</button>`)}
            <div class="table-wrap"><table>
                <thead><tr><th>申请</th><th>用户卡券</th><th>用户</th><th>门店</th><th>核销说明</th><th>状态</th><th>申请时间</th><th>审核</th></tr></thead>
                <tbody>${rows || emptyRow(8)}</tbody>
            </table></div>${pager("coupons", result)}`;
    }

    async function editCouponTemplate(id) {
        const template = state.couponTemplates.find((item) => item.id === id) || {};
        const storeId = Number(state.filters.coupons?.storeId);
        const saleDetail = template.saleProductId
            ? await api(`/admin-api/catalog/product/${template.saleProductId}`).catch(() => null)
            : null;
        const saleSku = saleDetail?.skus?.[0] || {
            priceFen: template.salePriceFen,
            stock: template.saleStock
        };
        const categoryOptions = (state.couponCategories || []).map((item) =>
            `<option value="${item.id}" ${Number(template.categoryId) === Number(item.id) ? "selected" : ""}>${escapeHtml(item.name)}</option>`
        ).join("");
        openDialog(id ? "编辑卡券模板" : "新增卡券模板", `
            <div class="form-grid">
                <label class="field full"><span>卡券名称</span><input name="name" value="${escapeHtml(template.name || "")}" maxlength="128" required></label>
                <label class="field full"><span>卡券说明</span><textarea name="description" maxlength="500">${escapeHtml(template.description || "")}</textarea></label>
                <label class="field"><span>卡券分类</span><select name="categoryId"><option value="">请选择分类</option>${categoryOptions}</select></label>
                <label class="field"><span>关联在售商品 ID</span><input name="saleProductId" type="number" min="1" value="${template.saleProductId || ""}" placeholder="购买后自动发券"></label>
                <label class="field"><span>售卖金额（元）</span><input name="salePriceYuan" type="number" min="0" step="0.01" value="${saleSku?.priceFen != null ? fenToYuan(saleSku.priceFen) : ""}" placeholder="例如 39.00"></label>
                <label class="field"><span>售卖库存</span><input name="saleStock" type="number" min="0" step="1" value="${saleSku?.stock ?? ""}" placeholder="例如 100"></label>
                <label class="field"><span>状态</span><select name="status"><option value="1" ${template.status !== 0 ? "selected" : ""}>启用</option><option value="0" ${template.status === 0 ? "selected" : ""}>停用</option></select></label>
                <label class="field"><span>购买有效天数</span><input name="purchaseValidDays" type="number" min="1" value="${template.purchaseValidDays || 365}" required></label>
                <label class="field"><span>赠送有效天数</span><input name="giftValidDays" type="number" min="1" value="${template.giftValidDays || 30}" required></label>
                <div class="field-note full">售卖金额和库存会同步到“关联在售商品 ID”的第一个 SKU；小程序卡券中心展示的购买价也来自这里。</div>
                <label class="field full"><span>卡券图片</span>
                    <div class="managed-image-row">
                        <div id="couponImagePreview" class="managed-image-preview">${template.imageUrl ? `<img src="${escapeHtml(assetUrl(template.imageUrl))}" alt="">` : "等待上传"}</div>
                        <div class="managed-image-controls">
                            <input id="couponImageUrl" name="imageUrl" value="${escapeHtml(template.imageUrl || "")}" placeholder="图片地址">
                            <label class="compact-upload">上传图片<input type="file" accept="image/*" onchange="adminApp.previewManagedImage(this, 'couponImageUrl', 'couponImagePreview')"></label>
                        </div>
                    </div>
                </label>
            </div>`, async (formData) => {
            const saleProductId = formData.get("saleProductId") ? Number(formData.get("saleProductId")) : null;
            await syncCouponSaleProduct(saleProductId, formData);
            await api("/admin-api/coupon/template", {
                method: id ? "PUT" : "POST",
                body: {
                    ...(id ? { id } : {}),
                    storeId,
                    categoryId: formData.get("categoryId") ? Number(formData.get("categoryId")) : null,
                    name: formData.get("name").trim(),
                    imageUrl: formData.get("imageUrl").trim(),
                    description: formData.get("description").trim(),
                    saleProductId,
                    purchaseValidDays: Number(formData.get("purchaseValidDays")),
                    giftValidDays: Number(formData.get("giftValidDays")),
                    status: Number(formData.get("status"))
                }
            });
            toast(id ? "卡券模板已更新" : "卡券模板已创建");
            await loadCoupons();
        });
    }

    async function syncCouponSaleProduct(saleProductId, formData) {
        const priceInput = String(formData.get("salePriceYuan") || "").trim();
        const stockInput = String(formData.get("saleStock") || "").trim();
        if (!saleProductId) {
            if (priceInput || stockInput) {
                throw new Error("填写售卖金额或库存前，请先填写关联在售商品 ID");
            }
            return;
        }
        if (!priceInput && !stockInput) {
            return;
        }
        const detail = await api(`/admin-api/catalog/product/${saleProductId}`);
        const product = detail.product;
        const storeId = Number(state.filters.coupons?.storeId || currentStoreId());
        if (storeId && Number(product.storeId) !== storeId) {
            throw new Error("关联商品不属于当前全局门店");
        }
        const skus = detail.skus && detail.skus.length
            ? detail.skus.map((sku) => ({ ...sku }))
            : [{ specName: "标准", priceFen: 0, stock: 0, sales: 0 }];
        const firstSku = skus[0];
        if (priceInput) {
            firstSku.priceFen = yuanToFen(priceInput);
        }
        if (stockInput) {
            const stock = Number(stockInput);
            if (!Number.isInteger(stock) || stock < 0) {
                throw new Error("售卖库存格式不正确");
            }
            firstSku.stock = stock;
        }
        firstSku.specName = firstSku.specName || "标准";
        await api("/admin-api/catalog/product", {
            method: "PUT",
            body: { product, skus }
        });
    }

    function giftCoupon(templateId) {
        const template = state.couponTemplates.find((item) => item.id === templateId);
        openDialog(`赠送${template ? `「${template.name}」` : "卡券"}`, `
            <div class="notice-card"><h4>商家赠送</h4><p>填写一个或多个用户 ID，多个 ID 使用英文逗号分隔。单次每位用户最多赠送 20 张。</p></div>
            <div class="form-grid top-gap">
                <label class="field full"><span>用户 ID</span><input name="userIds" placeholder="例如 10001,10002" required></label>
                <label class="field"><span>每人数量</span><input name="quantity" type="number" min="1" max="20" value="1" required></label>
            </div>`, async (formData) => {
            const userIds = formData.get("userIds").split(",").map(value => Number(value.trim())).filter(Boolean);
            await api("/admin-api/coupon/gift", {
                method: "POST",
                body: { templateId, userIds, quantity: Number(formData.get("quantity")) }
            });
            toast("卡券赠送完成");
        });
    }

    async function toggleCouponTemplate(id, status) {
        await runAction(async () => {
            await api(`/admin-api/coupon/template/status?id=${id}&status=${status}`, { method: "POST" });
            toast("卡券模板状态已更新");
            await loadCoupons();
        });
    }

    function auditCouponRedeem(requestId, approve) {
        openDialog(approve ? "通过卡券核销" : "拒绝卡券核销", `
            <div class="notice-card"><h4>核销申请 #${requestId}</h4><p>${approve ? "通过后卡券将变为已使用。" : "拒绝后，未过期卡券将恢复为可用状态。"}</p></div>
            <label class="field top-gap"><span>审核备注</span><textarea name="auditRemark" maxlength="255"></textarea></label>
        `, async (formData) => {
            await api("/admin-api/coupon/redeem-audit", {
                method: "POST",
                body: { requestId, approve, auditRemark: formData.get("auditRemark") }
            });
            toast("卡券核销申请已审核");
            await loadCouponRedeems();
        });
    }

    function formatDateInput(date) {
        return [
            date.getFullYear(),
            String(date.getMonth() + 1).padStart(2, "0"),
            String(date.getDate()).padStart(2, "0")
        ].join("-");
    }

    async function loadStatistics() {
        await ensureStores();
        const today = new Date();
        const thirtyDaysAgo = new Date(today);
        thirtyDaysAgo.setDate(today.getDate() - 29);
        const filters = state.filters.statistics || {
            storeId: currentStoreId(),
            startDate: formatDateInput(thirtyDaysAgo),
            endDate: formatDateInput(today)
        };
        filters.storeId = currentStoreId();
        state.filters.statistics = filters;
        const data = await api(`/admin-api/statistics/sales${toQuery(filters)}`);
        const dailyRows = data.dailySales.map((item) => `
            <tr><td>${escapeHtml(item.saleDate)}</td><td>${item.orderCount}</td><td class="money">${money(item.salesFen)}</td></tr>`).join("");
        const productRows = data.productSales.map((item, index) => `
            <tr><td>${index + 1}</td><td><span class="table-main">${escapeHtml(item.productName)}</span><span class="table-sub">${escapeHtml(item.specName || "-")}</span></td><td>${item.quantity}</td><td class="money">${money(item.salesFen)}</td></tr>`).join("");
        document.getElementById("view-statistics").innerHTML = `
            ${filterToolbar(`
                <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || filters.storeId || "-")}" disabled></label>
                <label class="field"><span>开始日期</span><input id="statisticsStartDate" type="date" value="${escapeHtml(filters.startDate)}"></label>
                <label class="field"><span>结束日期</span><input id="statisticsEndDate" type="date" value="${escapeHtml(filters.endDate)}"></label>
                <button class="primary-button" onclick="adminApp.searchStatistics()" type="button">统计</button>`)}
            <div class="metric-grid">
                <article class="metric-card"><span>销售额</span><strong>${money(data.salesFen)}</strong><small>${data.startDate} 至 ${data.endDate}</small></article>
                <article class="metric-card"><span>有效订单</span><strong>${data.orderCount}</strong><small>已支付及已完成</small></article>
                <article class="metric-card"><span>售出件数</span><strong>${data.itemQuantity}</strong><small>按订单商品数量汇总</small></article>
                <article class="metric-card"><span>订单均价</span><strong>${money(data.orderCount ? Math.round(data.salesFen / data.orderCount) : 0)}</strong><small>销售额 / 有效订单</small></article>
            </div>
            <div class="statistics-grid">
                <article class="panel"><div class="panel-head"><div><p class="panel-kicker">DAILY SALES</p><h3>每日销售</h3></div></div>
                    <div class="table-wrap"><table><thead><tr><th>日期</th><th>订单数</th><th>销售额</th></tr></thead><tbody>${dailyRows || emptyRow(3)}</tbody></table></div>
                </article>
                <article class="panel"><div class="panel-head"><div><p class="panel-kicker">PRODUCT RANKING</p><h3>商品销量排行</h3></div></div>
                    <div class="table-wrap"><table><thead><tr><th>排名</th><th>商品</th><th>销量</th><th>销售额</th></tr></thead><tbody>${productRows || emptyRow(4)}</tbody></table></div>
                </article>
            </div>`;
    }

    function storePointsTabs(active) {
        return `<div class="section-tabs">
            <button class="${active === "users" ? "active" : ""}" onclick="adminApp.switchStorePointsTab('users')" type="button">用户积分</button>
            <button class="${active === "requests" ? "active" : ""}" onclick="adminApp.switchStorePointsTab('requests')" type="button">存取审核</button>
            <button class="${active === "logs" ? "active" : ""}" onclick="adminApp.switchStorePointsTab('logs')" type="button">积分流水</button>
        </div>`;
    }

    function halvingRuleNotice() {
        return `<div class="halving-rule-banner">
            <strong>积分减半说明</strong>
            <span>后台可按门店配置任意间隔天数；每到一个自然日周期自动减半，减半结果四舍五入。</span>
        </div>`;
    }

    async function loadStorePoints() {
        await ensureStores();
        if (state.storePointsTab === "users") {
            await loadStorePointUsers();
            return;
        }
        if (state.storePointsTab === "logs") {
            await loadStorePointsLogs();
            return;
        }
        const filters = state.filters.storePoints || {};
        filters.storeId = currentStoreId();
        const result = await api(`/admin-api/store-points/requests${toQuery({
            storeId: filters.storeId,
            userId: filters.userId,
            status: filters.status,
            current: pageOf("storePoints"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td>#${item.id}</td><td>${item.storeId}</td>
                <td>${item.userId}</td>
                <td>${escapeHtml(item.nickname || "-")}</td>
                <td>${escapeHtml(item.phone || "-")}</td>
                <td>${item.type === "DEPOSIT" ? "存入" : "取出"}</td>
                <td><button class="points-link-button compact" onclick="adminApp.openStorePointUser(${item.userId})" type="button">${item.points}</button></td><td>${escapeHtml(item.remark || "-")}</td>
                <td>${statusBadge(item.status)}</td><td>${dateTime(item.createTime)}</td>
                <td>${item.status === "PENDING" ? `<div class="row-actions">
                    <button class="table-button primary" onclick="adminApp.auditStorePoints(${item.id}, '${item.type}', true)" type="button">通过</button>
                    <button class="table-button danger" onclick="adminApp.auditStorePoints(${item.id}, '${item.type}', false)" type="button">拒绝</button>
                </div>` : escapeHtml(item.auditRemark || "-")}</td>
            </tr>`).join("");
        document.getElementById("view-storePoints").innerHTML = `
            ${storePointsTabs("requests")}
            ${halvingRuleNotice()}
            <div class="toolbar product-filter-toolbar">
                <div class="filters">
                    <label class="field wide"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || filters.storeId || "-")}" disabled></label>
                    <label class="field"><span>用户 ID</span><input id="storePointsUserId" type="number" value="${escapeHtml(filters.userId || "")}"></label>
                    <label class="field"><span>状态</span><select id="storePointsStatus">${statusOptions(filters.status, ["PENDING", "APPROVED", "REJECTED"])}</select></label>
                    <button class="primary-button" onclick="adminApp.searchStorePoints()" type="button">查询</button>
                </div>
                <div class="toolbar-actions">
                    <button class="primary-button" onclick="adminApp.setStorePoints()" type="button">直接设置积分</button>
                    <button class="secondary-button" onclick="adminApp.adjustStorePoints()" type="button">发放 / 调整积分</button>
                </div>
            </div>
            <div class="table-wrap"><table>
                <thead><tr><th>申请</th><th>门店</th><th>用户 ID</th><th>用户名</th><th>手机号</th><th>类型</th><th>积分</th><th>用途说明</th><th>状态</th><th>申请时间</th><th>审核</th></tr></thead>
                <tbody>${rows || emptyRow(11)}</tbody>
            </table></div>${pager("storePoints", result)}`;
    }

    async function loadStorePointUsers() {
        const filters = state.filters.storePointUsers || {};
        filters.storeId = currentStoreId();
        const result = await api(`/admin-api/store-points/users${toQuery({
            storeId: filters.storeId,
            keyword: filters.keyword,
            current: pageOf("storePoints"),
            size: 10
        })}`);
        state.storePointUsers = result.records || [];
        const rows = state.storePointUsers.map((item) => `
            <tr>
                <td><span class="table-main">#${item.userId}</span><span class="table-sub">${escapeHtml(item.nickname || "未设置昵称")}</span></td>
                <td>${escapeHtml(item.phone || "-")}</td>
                <td>${userStatusBadge(item.status)}</td>
                <td>${item.storeCount ?? 0}</td>
                <td><button class="points-link-button" onclick="adminApp.openStorePointUser(${item.userId})" type="button">${item.totalPoints ?? 0}</button></td>
                <td>${item.frozenPoints ?? 0}</td>
                <td>${dateTime(item.lastChangeTime)}</td>
                <td><div class="row-actions">
                    <button class="table-button primary" onclick="adminApp.openStorePointUser(${item.userId})" type="button">记录</button>
                    <button class="table-button" onclick="adminApp.setStorePoints(${item.userId})" type="button">设置</button>
                </div></td>
            </tr>`).join("");
        document.getElementById("view-storePoints").innerHTML = `
            ${storePointsTabs("users")}
            ${halvingRuleNotice()}
            <div class="toolbar product-filter-toolbar">
                <div class="filters">
                    <label class="field wide"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || filters.storeId || "-")}" disabled></label>
                    <label class="field wide"><span>用户</span><input id="storePointUserKeyword" value="${escapeHtml(filters.keyword || "")}" placeholder="用户 ID、昵称或手机号"></label>
                    <button class="primary-button" onclick="adminApp.searchStorePointUsers()" type="button">查询</button>
                </div>
                <div class="toolbar-actions">
                    <button class="primary-button" onclick="adminApp.setStorePoints()" type="button">直接设置积分</button>
                    <button class="secondary-button" onclick="adminApp.adjustStorePoints()" type="button">发放 / 调整积分</button>
                </div>
            </div>
            <div class="table-wrap"><table>
                <thead><tr><th>用户</th><th>手机号</th><th>状态</th><th>门店数</th><th>总积分</th><th>冻结积分</th><th>最近变动</th><th>操作</th></tr></thead>
                <tbody>${rows || emptyRow(8)}</tbody>
            </table></div>${pager("storePoints", result)}`;
    }

    async function loadStorePointsLogs() {
        const filters = state.filters.storePointsLogs || {};
        const storeId = currentStoreId();
        filters.storeId = storeId;
        const userId = filters.userId || "";
        let result = { records: [], current: 1, size: 10, total: 0 };
        if (storeId) {
            result = await api(`/admin-api/store-points/logs${toQuery({
                storeId, userId, current: pageOf("storePoints"), size: 10
            })}`);
        } else if (userId) {
            result = await api(`/admin-api/store-points/logs${toQuery({
                userId, current: pageOf("storePoints"), size: 10
            })}`);
        }
        const rows = result.records.map((item) => `
            <tr><td>#${item.id}</td><td>${item.storeId}</td><td><span class="table-main">#${item.userId}</span><span class="table-sub">${escapeHtml(item.nickname || "-")} / ${escapeHtml(item.phone || "-")}</span></td>
                <td>${escapeHtml(item.changeType)}</td><td>${item.changeValue > 0 ? "+" : ""}${item.changeValue}</td>
                <td>${item.beforeValue} → ${item.afterValue}</td><td>${escapeHtml(item.remark || "-")}</td><td>${dateTime(item.createTime)}</td></tr>`).join("");
        document.getElementById("view-storePoints").innerHTML = `
            ${storePointsTabs("logs")}
            ${halvingRuleNotice()}
            ${filterToolbar(`
                <label class="field wide"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || storeId || "-")}" disabled></label>
                <label class="field"><span>用户 ID</span><input id="storePointsLogUserId" type="number" value="${escapeHtml(userId)}" required></label>
                <button class="primary-button" onclick="adminApp.searchStorePointsLogs()" type="button">查询</button>`)}
            <div class="table-wrap"><table>
                <thead><tr><th>流水</th><th>门店</th><th>用户</th><th>类型</th><th>变动</th><th>变动前后</th><th>说明</th><th>时间</th></tr></thead>
                <tbody>${rows || emptyRow(8, "暂无积分流水")}</tbody>
            </table></div>${pager("storePoints", result)}`;
    }

    function auditStorePoints(requestId, type, approve) {
        const isDeposit = type === "DEPOSIT";
        const actionName = isDeposit ? "存入" : "取出";
        const ruleText = isDeposit
            ? (approve ? "通过后积分才会累计到用户的本店积分池。" : "拒绝后不会增加用户积分。")
            : (approve ? "通过后将正式扣减此前冻结的积分。" : "拒绝后将解冻积分并退回用户可用积分。");
        openDialog(`${approve ? "通过" : "拒绝"}积分${actionName}`, `
            <div class="notice-card"><h4>门店积分申请 #${requestId}</h4><p>${ruleText}</p></div>
            <label class="field top-gap"><span>审核备注</span><textarea name="auditRemark" maxlength="255"></textarea></label>
        `, async (formData) => {
            await api("/admin-api/store-points/audit", {
                method: "POST",
                body: { requestId, approve, auditRemark: formData.get("auditRemark") }
            });
            toast("门店积分申请已审核");
            await loadStorePoints();
        });
    }

    function adjustStorePoints() {
        const defaultStoreId = currentStoreId();
        openDialog("发放 / 调整门店积分", `
            <div class="notice-card"><h4>门店积分调整</h4><p>正数为发放，负数为扣减。积分仅影响指定门店的排行榜和用户积分账户。</p></div>
            <div class="form-grid top-gap">
                <input name="storeId" type="hidden" value="${defaultStoreId}">
                <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || defaultStoreId || "-")}" disabled></label>
                <label class="field"><span>用户 ID</span><input name="userId" type="number" min="1" required></label>
                <label class="field"><span>调整值</span><input name="value" type="number" required></label>
                <label class="field full"><span>调整原因</span><textarea name="remark" maxlength="255" required></textarea></label>
            </div>`, async (formData) => {
            await api("/admin-api/store-points/adjust", {
                method: "POST",
                body: {
                    storeId: Number(formData.get("storeId")),
                    userId: Number(formData.get("userId")),
                    value: Number(formData.get("value")),
                    remark: formData.get("remark").trim()
                }
            });
            toast("门店积分已调整");
            await loadStorePoints();
        });
    }

    async function openStorePointUser(userId) {
        try {
            const filters = state.filters.storePointUsers || {};
            const storeId = currentStoreId();
            let summary = state.storePointUsers.find((item) => Number(item.userId) === Number(userId));
            if (!summary) {
                const summaryPage = await api(`/admin-api/store-points/users${toQuery({
                    storeId, keyword: userId, current: 1, size: 1
                })}`);
                summary = summaryPage.records?.[0];
            }
            const result = await api(`/admin-api/store-points/logs${toQuery({
                storeId, userId, current: 1, size: 20
            })}`);
            const rows = result.records.map((item) => `
                <tr>
                    <td>#${item.id}</td><td>${item.storeId}</td>
                    <td>${escapeHtml(item.changeType)}</td>
                    <td>${item.changeValue > 0 ? "+" : ""}${item.changeValue}</td>
                    <td>${item.beforeValue} → ${item.afterValue}</td>
                    <td>${escapeHtml(item.operator || "-")}</td>
                    <td>${escapeHtml(item.remark || "-")}</td>
                    <td>${dateTime(item.createTime)}</td>
                </tr>`).join("");
            openDialog("用户积分记录", `
                ${detailGrid([
                    ["用户 ID", summary?.userId || userId],
                    ["昵称", summary?.nickname || "-"],
                    ["手机号", summary?.phone || "-"],
                    ["状态", summary?.status === 0 ? "正常" : "已停用"],
                    ["总积分", summary?.totalPoints ?? "-"],
                    ["冻结积分", summary?.frozenPoints ?? "-"]
                ])}
                <div class="table-wrap top-gap"><table>
                    <thead><tr><th>流水</th><th>门店</th><th>类型</th><th>变动</th><th>变动前后</th><th>操作人</th><th>说明</th><th>时间</th></tr></thead>
                    <tbody>${rows || emptyRow(8, "暂无积分记录")}</tbody>
                </table></div>
                <div class="field-note top-gap">当前展示最近 20 条积分记录；如需完整流水可切换到“积分流水”页按用户查询。</div>
            `, null);
        } catch (error) {
            toast(error.message, "error");
        }
    }

    function setStorePoints(defaultUserId = "") {
        const defaultStoreId = currentStoreId();
        openDialog("直接设置门店积分", `
            <div class="notice-card danger-note"><h4>直接设置积分</h4><p>提交后会把用户在指定门店的积分设置为目标值，并记录积分流水。目标值不能低于该用户当前冻结积分。</p></div>
            <div class="form-grid top-gap">
                <input name="storeId" type="hidden" value="${defaultStoreId}">
                <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || defaultStoreId || "-")}" disabled></label>
                <label class="field"><span>用户 ID</span><input name="userId" type="number" min="1" value="${escapeHtml(defaultUserId)}" required></label>
                <label class="field"><span>目标积分</span><input name="points" type="number" min="0" step="1" required></label>
                <label class="field full"><span>设置原因</span><textarea name="remark" maxlength="255" required></textarea></label>
            </div>`, async (formData) => {
            await api("/admin-api/store-points/set", {
                method: "POST",
                body: {
                    storeId: Number(formData.get("storeId")),
                    userId: Number(formData.get("userId")),
                    points: Number(formData.get("points")),
                    remark: formData.get("remark").trim()
                }
            });
            toast("门店积分已设置");
            await loadStorePoints();
        });
    }

    async function loadRecharges() {
        if (state.rechargeTab === "tiers") {
            await loadRechargeTiers();
            return;
        }
        await ensureStores();
        const filters = state.filters.recharges || {};
        const storeId = currentStoreId();
        const result = await api(`/admin-api/recharge/page${toQuery({
            storeId,
            rechargeNo: filters.rechargeNo,
            status: filters.status,
            current: pageOf("recharges"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td><span class="table-main">${escapeHtml(item.rechargeNo)}</span></td>
                <td><span class="table-main">#${item.userId}</span><span class="table-sub">${escapeHtml(item.nickname || "-")} / ${escapeHtml(item.phone || "-")}</span></td>
                <td>${item.tierId}</td>
                <td class="money">${money(item.payFen)}</td>
                <td class="money">${money(item.bonusFen)}</td>
                <td>${statusBadge(item.status)}</td>
                <td>${item.credited === 1 ? '<span class="badge success">已入账</span>' : '<span class="badge warning">未入账</span>'}</td>
                <td>${dateTime(item.createTime)}</td>
                <td>${item.status === "PAID" && item.credited !== 1 ? `<button class="table-button primary" onclick="adminApp.manualCredit('${escapeHtml(item.rechargeNo)}')" type="button">手工入账</button>` : "-"}</td>
            </tr>`).join("");
        document.getElementById("view-recharges").innerHTML = `
            ${rechargeTabs("orders")}
            ${filterToolbar(`
                <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || storeId || "-")}" disabled></label>
                <label class="field wide"><span>充值单号</span><input id="rechargeNo" value="${escapeHtml(filters.rechargeNo || "")}" placeholder="输入充值单号"></label>
                <label class="field"><span>状态</span><select id="rechargeStatus">${statusOptions(filters.status, ["CREATED", "PAID"])}</select></label>
                <button class="primary-button" onclick="adminApp.searchRecharges()" type="button">查询</button>`)}
            <div class="table-wrap"><table>
                <thead><tr><th>充值单</th><th>用户</th><th>档位</th><th>支付金额</th><th>赠送金额</th><th>状态</th><th>入账</th><th>创建时间</th><th>操作</th></tr></thead>
                <tbody>${rows || emptyRow(9)}</tbody>
            </table></div>${pager("recharges", result)}`;
    }

    async function loadRechargeTiers() {
        const tiers = await api("/admin-api/recharge/tiers");
        state.rechargeTiers = tiers;
        document.getElementById("view-recharges").innerHTML = `
            ${rechargeTabs("tiers")}
            <div class="toolbar product-filter-toolbar"><div></div><button class="primary-button" onclick="adminApp.editTier()" type="button">新增充值档位</button></div>
            <div class="table-wrap"><table>
                <thead><tr><th>档位 ID</th><th>展示名称</th><th>支付金额</th><th>赠送金额</th><th>状态</th><th>排序</th><th>操作</th></tr></thead>
                <tbody>${tiers.map((item) => `<tr>
                    <td>#${item.id}</td><td class="table-main">${escapeHtml(item.label)}</td><td class="money">${money(item.payFen)}</td><td class="money">${money(item.bonusFen)}</td><td>${statusBadge(item.status)}</td><td>${item.sort}</td>
                    <td><div class="row-actions"><button class="table-button primary" onclick="adminApp.editTierById(${item.id})" type="button">编辑</button><button class="table-button danger" onclick="adminApp.deleteTier(${item.id})" type="button">删除</button></div></td>
                </tr>`).join("") || emptyRow(7)}</tbody>
            </table></div>`;
    }

    function rechargeTabs(active) {
        return `<div class="section-tabs">
            <button class="${active === "orders" ? "active" : ""}" onclick="adminApp.switchRechargeTab('orders')" type="button">充值订单</button>
            <button class="${active === "tiers" ? "active" : ""}" onclick="adminApp.switchRechargeTab('tiers')" type="button">充值档位</button>
        </div>`;
    }

    function editTier(tier = {}) {
        openDialog(tier.id ? "编辑充值档位" : "新增充值档位", `
            <div class="form-grid">
                <label class="field full"><span>展示名称</span><input name="label" value="${escapeHtml(tier.label || "")}" required></label>
                <label class="field"><span>支付金额（分）</span><input name="payFen" type="number" min="1" value="${tier.payFen || ""}" required></label>
                <label class="field"><span>赠送金额（分）</span><input name="bonusFen" type="number" min="0" value="${tier.bonusFen || 0}" required></label>
                <label class="field"><span>状态</span><select name="status"><option value="1" ${tier.status !== 0 ? "selected" : ""}>启用</option><option value="0" ${tier.status === 0 ? "selected" : ""}>停用</option></select></label>
                <label class="field"><span>排序</span><input name="sort" type="number" value="${tier.sort || 0}" required></label>
            </div>`, async (formData) => {
            await api("/admin-api/recharge/tier", {
                method: tier.id ? "PUT" : "POST",
                body: {
                    ...(tier.id ? { id: tier.id } : {}),
                    label: formData.get("label"),
                    payFen: Number(formData.get("payFen")),
                    bonusFen: Number(formData.get("bonusFen")),
                    status: Number(formData.get("status")),
                    sort: Number(formData.get("sort"))
                }
            });
            toast(tier.id ? "充值档位已更新" : "充值档位已新增");
            await loadRechargeTiers();
        });
    }

    async function deleteTier(id) {
        if (!window.confirm("确认删除该充值档位吗？")) return;
        await runAction(async () => {
            await api(`/admin-api/recharge/tier/${id}`, { method: "DELETE" });
            toast("充值档位已删除");
            await loadRechargeTiers();
        });
    }

    async function manualCredit(rechargeNo) {
        if (!window.confirm("确认对该充值单执行手工入账吗？该操作要求充值单已支付且未入账。")) return;
        await runAction(async () => {
            await api(`/admin-api/recharge/manual-credit/${encodeURIComponent(rechargeNo)}`, { method: "POST" });
            toast("手工入账完成");
            await loadRecharges();
        });
    }

    async function loadPoints() {
        await ensureStores();
        const filters = state.filters.points || {};
        const storeId = currentStoreId();
        const result = await api(`/admin-api/points/page${toQuery({
            storeId,
            userId: filters.userId,
            type: filters.type,
            status: filters.status,
            current: pageOf("points"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td>#${item.id}</td>
                <td><span class="table-main">#${item.userId}</span><span class="table-sub">${escapeHtml(item.nickname || "-")} / ${escapeHtml(item.phone || "-")}</span></td>
                <td>${escapeHtml(item.type)}</td>
                <td>${item.points}</td>
                <td>${escapeHtml(item.remark || "-")}</td>
                <td>${statusBadge(item.status)}</td>
                <td>${dateTime(item.createTime)}</td>
                <td>${item.status === "PENDING" ? `<div class="row-actions"><button class="table-button primary" onclick="adminApp.auditPoints(${item.id}, true)" type="button">通过</button><button class="table-button danger" onclick="adminApp.auditPoints(${item.id}, false)" type="button">拒绝</button></div>` : escapeHtml(item.auditRemark || "-")}</td>
            </tr>`).join("");
        document.getElementById("view-points").innerHTML = `
            ${filterToolbar(`
                <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || storeId || "-")}" disabled></label>
                <label class="field"><span>用户 ID</span><input id="pointsUserId" type="number" value="${escapeHtml(filters.userId || "")}" placeholder="用户 ID"></label>
                <label class="field"><span>类型</span><input id="pointsType" value="${escapeHtml(filters.type || "")}" placeholder="例如 MANUAL"></label>
                <label class="field"><span>状态</span><select id="pointsStatus">${statusOptions(filters.status, ["PENDING", "APPROVED", "REJECTED"])}</select></label>
                <button class="primary-button" onclick="adminApp.searchPoints()" type="button">查询</button>`)}
            <div class="table-wrap"><table>
                <thead><tr><th>申请 ID</th><th>用户</th><th>类型</th><th>积分</th><th>申请说明</th><th>状态</th><th>申请时间</th><th>审核</th></tr></thead>
                <tbody>${rows || emptyRow(8)}</tbody>
            </table></div>${pager("points", result)}`;
    }

    function auditPoints(requestId, approve) {
        openDialog(approve ? "通过积分申请" : "拒绝积分申请", `
            <div class="notice-card"><h4>${approve ? "确认通过" : "确认拒绝"}申请 #${requestId}</h4><p>审核结果会影响用户积分账户，并记录当前管理员及操作时间。</p></div>
            <label class="field" style="margin-top:16px"><span>审核备注</span><textarea name="auditRemark" maxlength="255" placeholder="请输入审核说明"></textarea></label>
        `, async (formData) => {
            await api("/admin-api/points/audit", {
                method: "POST",
                body: { requestId, approve, auditRemark: formData.get("auditRemark") }
            });
            toast("积分申请已审核");
            await loadPoints();
        });
    }

    async function loadAccounts() {
        await ensureStores();
        const filters = state.filters.accounts || {};
        const storeId = currentStoreId();
        const result = await api(`/admin-api/account/logs${toQuery({
            storeId,
            userId: filters.userId,
            type: filters.type,
            bizNo: filters.bizNo,
            current: pageOf("accounts"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td>#${item.id}</td>
                <td><span class="table-main">#${item.userId}</span><span class="table-sub">${escapeHtml(item.nickname || "-")} / ${escapeHtml(item.phone || "-")}</span></td>
                <td><span class="table-main">${escapeHtml(item.assetType)}</span><span class="table-sub">${escapeHtml(item.changeType)}</span></td>
                <td class="money">${Number(item.changeValue) >= 0 ? "+" : ""}${item.changeValue}</td>
                <td>${item.beforeValue} → ${item.afterValue}</td>
                <td>${escapeHtml(item.bizNo || "-")}</td>
                <td>${escapeHtml(item.operator || "-")}</td>
                <td>${dateTime(item.createTime)}</td>
            </tr>`).join("");
        document.getElementById("view-accounts").innerHTML = `
            <div class="toolbar product-filter-toolbar">
                <div class="filters">
                    <label class="field"><span>当前门店</span><input value="${escapeHtml(currentStore()?.name || storeId || "-")}" disabled></label>
                    <label class="field"><span>用户 ID</span><input id="accountUserId" type="number" value="${escapeHtml(filters.userId || "")}"></label>
                    <label class="field"><span>变更类型</span><input id="accountType" value="${escapeHtml(filters.type || "")}" placeholder="例如 RECHARGE"></label>
                    <label class="field wide"><span>业务单号</span><input id="accountBizNo" value="${escapeHtml(filters.bizNo || "")}"></label>
                    <button class="primary-button" onclick="adminApp.searchAccounts()" type="button">查询</button>
                </div>
                <div class="toolbar-actions">
                    <button class="secondary-button" onclick="adminApp.exportAccounts()" type="button">导出 CSV</button>
                    <button class="primary-button" onclick="adminApp.adjustAccount()" type="button">资产调整</button>
                </div>
            </div>
            <div class="table-wrap"><table>
                <thead><tr><th>流水 ID</th><th>用户</th><th>资产 / 类型</th><th>变动值</th><th>变动前后</th><th>业务单号</th><th>操作方</th><th>时间</th></tr></thead>
                <tbody>${rows || emptyRow(8)}</tbody>
            </table></div>${pager("accounts", result)}`;
    }

    function adjustAccount() {
        openDialog("用户资产调整", `
            <div class="form-grid">
                <label class="field"><span>用户 ID</span><input name="userId" type="number" min="1" required></label>
                <label class="field"><span>资产类型</span><select name="type"><option value="BALANCE">账户余额</option><option value="POINTS">积分</option></select></label>
                <label class="field full"><span>调整值</span><input name="value" type="number" required><small class="field-note">余额单位为分，积分单位为个；正数增加，负数扣减。</small></label>
                <label class="field full"><span>调整原因</span><textarea name="remark" maxlength="255" required></textarea></label>
                <label class="field full"><span><input name="confirm" type="checkbox" value="true" style="width:auto;min-height:auto;margin-right:8px" required>我已核对用户、资产类型和调整值</span></label>
            </div>`, async (formData) => {
            await api("/admin-api/account/adjust", {
                method: "POST",
                body: {
                    userId: Number(formData.get("userId")),
                    type: formData.get("type"),
                    value: Number(formData.get("value")),
                    remark: formData.get("remark"),
                    confirm: formData.get("confirm") === "true"
                }
            });
            toast("用户资产已调整");
            await loadAccounts();
        });
    }

    async function exportAccounts() {
        try {
            const filters = state.filters.accounts || {};
            const blob = await api(`/admin-api/account/logs/export${toQuery({
                ...filters,
                storeId: currentStoreId()
            })}`, { blob: true });
            const url = URL.createObjectURL(blob);
            const anchor = document.createElement("a");
            anchor.href = url;
            anchor.download = `account-logs-${new Date().toISOString().slice(0, 10)}.csv`;
            anchor.click();
            URL.revokeObjectURL(url);
            toast("流水文件已导出");
        } catch (error) {
            toast(error.message, "error");
        }
    }

    async function loadOperations() {
        const filters = state.filters.operations || {};
        const result = await api(`/admin-api/operation/page${toQuery({
            adminId: filters.adminId,
            module: filters.module,
            startTime: filters.startTime,
            endTime: filters.endTime,
            current: pageOf("operations"),
            size: 10
        })}`);
        const rows = result.records.map((item) => `
            <tr>
                <td>#${item.id}</td>
                <td>${item.adminId}</td>
                <td><span class="table-main">${escapeHtml(item.module)}</span><span class="table-sub">${escapeHtml(item.action)}</span></td>
                <td>${escapeHtml(item.ip || "-")}</td>
                <td>${item.costMs ?? 0} ms</td>
                <td>${dateTime(item.createTime)}</td>
                <td><button class="table-button" onclick='adminApp.showJson("操作参数摘要", ${JSON.stringify(JSON.stringify(item.paramsDigest || "", null, 2))})' type="button">摘要</button></td>
            </tr>`).join("");
        document.getElementById("view-operations").innerHTML = `
            ${filterToolbar(`
                <label class="field"><span>管理员 ID</span><input id="operationAdminId" type="number" value="${escapeHtml(filters.adminId || "")}"></label>
                <label class="field"><span>业务模块</span><input id="operationModule" value="${escapeHtml(filters.module || "")}" placeholder="例如 ORDER"></label>
                <label class="field"><span>开始时间</span><input id="operationStart" type="datetime-local" value="${escapeHtml(filters.startTime || "")}"></label>
                <label class="field"><span>结束时间</span><input id="operationEnd" type="datetime-local" value="${escapeHtml(filters.endTime || "")}"></label>
                <button class="primary-button" onclick="adminApp.searchOperations()" type="button">查询</button>`)}
            <div class="table-wrap"><table>
                <thead><tr><th>日志 ID</th><th>管理员</th><th>模块 / 动作</th><th>IP</th><th>耗时</th><th>操作时间</th><th>参数</th></tr></thead>
                <tbody>${rows || emptyRow(7)}</tbody>
            </table></div>${pager("operations", result)}`;
    }

    async function loadReconcile() {
        const today = new Date();
        today.setDate(today.getDate() - 1);
        const defaultDate = today.toISOString().slice(0, 10);
        document.getElementById("view-reconcile").innerHTML = `
            <div class="reconcile-card">
                <div>
                    <div class="stat-symbol" style="margin:auto;width:48px;height:48px">核</div>
                    <h3>每日交易对账</h3>
                    <p>按业务日期核对订单与支付记录。建议对昨日数据执行对账，避免当日交易尚未全部完成造成差异。</p>
                    <div class="reconcile-form">
                        <input id="reconcileDate" type="date" value="${defaultDate}">
                        <button class="primary-button" onclick="adminApp.runReconcile()" type="button">开始对账</button>
                    </div>
                    <div id="reconcileResult" class="result-box"></div>
                </div>
            </div>`;
    }

    async function runReconcile() {
        const date = document.getElementById("reconcileDate").value;
        if (!date) {
            toast("请选择对账日期", "error");
            return;
        }
        await runAction(async () => {
            const result = await api(`/admin-api/reconcile/daily?date=${date}`, { method: "POST" });
            document.getElementById("reconcileResult").innerHTML = `
                <div class="notice-card"><h4>${date} 对账完成</h4><pre class="json-view">${escapeHtml(JSON.stringify(result, null, 2))}</pre></div>`;
            toast("对账已完成");
        });
    }

    function filterToolbar(content) {
        return `<div class="toolbar product-filter-toolbar"><div class="filters">${content}</div></div>`;
    }

    function statusOptions(selected, values) {
        return `<option value="">全部状态</option>` + values.map((value) =>
            `<option value="${value}" ${value === selected ? "selected" : ""}>${value}</option>`
        ).join("");
    }

    function detailGrid(items) {
        return `<div class="detail-grid">${items.map(([label, value]) =>
            `<div class="detail-item"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value ?? "-")}</strong></div>`
        ).join("")}</div>`;
    }

    function openDialog(title, body, submitHandler, submitText = "确认") {
        document.getElementById("dialogTitle").textContent = title;
        document.getElementById("dialogBody").innerHTML = body;
        document.getElementById("submitDialog").textContent = submitText;
        document.getElementById("dialogActions").classList.toggle("hidden", !submitHandler);
        state.dialogSubmit = submitHandler;
        dialog.showModal();
    }

    function closeDialog() {
        dialog.close();
        state.dialogSubmit = null;
        dialogForm.reset();
    }

    async function runAction(action) {
        setLoading(true);
        try {
            await action();
        } catch (error) {
            toast(error.message || "操作失败", "error");
        } finally {
            setLoading(false);
        }
    }

    function search(view, filters) {
        state.filters[view] = filters;
        state.pages[view] = 1;
        loadView(view).catch((error) => toast(error.message, "error"));
    }

    document.getElementById("loginForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = document.getElementById("loginButton");
        const errorNode = document.getElementById("loginError");
        button.disabled = true;
        button.textContent = "正在登录...";
        errorNode.textContent = "";
        try {
            const data = await api("/admin-api/auth/login", {
                method: "POST",
                public: true,
                body: {
                    username: document.getElementById("loginUsername").value.trim(),
                    password: document.getElementById("loginPassword").value
                }
            });
            saveSession(data);
            enterApp();
        } catch (error) {
            errorNode.textContent = error.message || "登录失败";
        } finally {
            button.disabled = false;
            button.textContent = "进入后台";
        }
    });

    document.getElementById("togglePassword").addEventListener("click", () => {
        const input = document.getElementById("loginPassword");
        const visible = input.type === "text";
        input.type = visible ? "password" : "text";
        document.getElementById("togglePassword").textContent = visible ? "显示" : "隐藏";
    });

    document.querySelectorAll(".nav-item").forEach((node) => {
        node.addEventListener("click", () => navigate(node.dataset.view));
    });
    document.querySelectorAll("[data-jump]").forEach((node) => {
        node.addEventListener("click", () => navigate(node.dataset.jump));
    });
    document.getElementById("refreshButton").addEventListener("click", () => {
        loadView(state.currentView)
            .then(() => toast("数据已刷新"))
            .catch((error) => toast(error.message, "error"));
    });
    ["globalStoreSelect", "dashboardStoreSelect"].forEach((id) => {
        const select = document.getElementById(id);
        if (!select) return;
        select.addEventListener("change", (event) => {
            changeGlobalStore(event.target.value).catch((error) => toast(error.message, "error"));
        });
    });
    document.getElementById("logoutButton").addEventListener("click", () => logout());
    document.getElementById("openSidebar").addEventListener("click", () => document.getElementById("sidebar").classList.add("open"));
    document.getElementById("closeSidebar").addEventListener("click", () => document.getElementById("sidebar").classList.remove("open"));
    document.getElementById("closeDialog").addEventListener("click", closeDialog);
    document.getElementById("cancelDialog").addEventListener("click", closeDialog);
    dialogForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!state.dialogSubmit) return;
        const submit = document.getElementById("submitDialog");
        submit.disabled = true;
        try {
            await state.dialogSubmit(new FormData(dialogForm));
            closeDialog();
        } catch (error) {
            toast(error.message || "操作失败", "error");
        } finally {
            submit.disabled = false;
        }
    });

    const now = new Date();
    document.getElementById("currentDate").textContent = now.toLocaleDateString("zh-CN", {
        year: "numeric", month: "long", day: "numeric", weekday: "short"
    });
    const heroDay = document.getElementById("heroDay");
    const heroMonth = document.getElementById("heroMonth");
    if (heroDay) heroDay.textContent = String(now.getDate()).padStart(2, "0");
    if (heroMonth) heroMonth.textContent = now.toLocaleDateString("en-US", { month: "long", year: "numeric" });

    window.adminApp = {
        jump: navigate,
        setPage,
        userDetail,
        toggleUser,
        editProduct,
        toggleProduct,
        editStore,
        toggleStoreStatus,
        manageCategories,
        renameCategory,
        deleteCategory,
        editAnnouncement,
        orderDetail,
        completeOrder,
        paymentDetail,
        previewManagedImage,
        saveStoreConfig,
        editActivity,
        toggleActivity,
        editCouponTemplate,
        giftCoupon,
        toggleCouponTemplate,
        auditCouponRedeem,
        auditStorePoints,
        adjustStorePoints,
        setStorePoints,
        openStorePointUser,
        manualCredit,
        editTier,
        editTierById: (id) => editTier(state.rechargeTiers.find((item) => item.id === id) || {}),
        deleteTier,
        auditPoints,
        adjustAccount,
        exportAccounts,
        runReconcile,
        showJson: (title, value) => openDialog(title, `<pre class="json-view">${escapeHtml(value)}</pre>`, null),
        changeContentStore: (storeId) => changeGlobalStore(storeId).catch((error) => toast(error.message, "error")),
        switchCouponTab: (tab) => {
            state.couponTab = tab;
            state.pages.coupons = 1;
            loadCoupons().catch((error) => toast(error.message, "error"));
        },
        changeCouponStore: (storeId) => changeGlobalStore(storeId).catch((error) => toast(error.message, "error")),
        switchStorePointsTab: (tab) => {
            state.storePointsTab = tab;
            state.pages.storePoints = 1;
            loadStorePoints().catch((error) => toast(error.message, "error"));
        },
        switchRechargeTab: (tab) => {
            state.rechargeTab = tab;
            state.pages.recharges = 1;
            loadRecharges().catch((error) => toast(error.message, "error"));
        },
        searchUsers: () => search("users", { keyword: document.getElementById("userKeyword").value.trim() }),
        searchProducts: () => search("products", {
            storeId: currentStoreId(),
            categoryId: document.getElementById("productCategory").value,
            keyword: document.getElementById("productKeyword").value.trim()
        }),
        searchStores: () => search("stores", {
            keyword: document.getElementById("storeKeyword").value.trim(),
            status: document.getElementById("storeStatus").value
        }),
        searchOrders: () => search("orders", {
            storeId: currentStoreId(),
            orderNo: document.getElementById("orderNo").value.trim(),
            status: document.getElementById("orderStatus").value
        }),
        searchPayments: () => search("payments", {
            storeId: currentStoreId(),
            orderNo: document.getElementById("paymentOrderNo").value.trim(),
            tradeState: document.getElementById("paymentState").value.trim()
        }),
        searchRecharges: () => search("recharges", {
            storeId: currentStoreId(),
            rechargeNo: document.getElementById("rechargeNo").value.trim(),
            status: document.getElementById("rechargeStatus").value
        }),
        searchIssuedCoupons: () => {
            state.filters.couponIssued = {
                storeId: currentStoreId(),
                userId: document.getElementById("couponIssuedUserId").value,
                status: document.getElementById("couponIssuedStatus").value
            };
            state.pages.coupons = 1;
            loadIssuedCoupons().catch((error) => toast(error.message, "error"));
        },
        searchCouponRedeems: () => {
            state.filters.couponRedeem = {
                storeId: currentStoreId(),
                status: document.getElementById("couponRedeemStatus").value
            };
            state.pages.coupons = 1;
            loadCouponRedeems().catch((error) => toast(error.message, "error"));
        },
        searchStatistics: () => search("statistics", {
            storeId: currentStoreId(),
            startDate: document.getElementById("statisticsStartDate").value,
            endDate: document.getElementById("statisticsEndDate").value
        }),
        searchStorePointUsers: () => {
            state.filters.storePointUsers = {
                storeId: currentStoreId(),
                keyword: document.getElementById("storePointUserKeyword").value.trim()
            };
            state.pages.storePoints = 1;
            loadStorePointUsers().catch((error) => toast(error.message, "error"));
        },
        searchStorePoints: () => search("storePoints", {
            storeId: currentStoreId(),
            userId: document.getElementById("storePointsUserId").value,
            status: document.getElementById("storePointsStatus").value
        }),
        searchStorePointsLogs: () => {
            state.filters.storePointsLogs = {
                storeId: currentStoreId(),
                userId: document.getElementById("storePointsLogUserId").value
            };
            state.pages.storePoints = 1;
            loadStorePointsLogs().catch((error) => toast(error.message, "error"));
        },
        searchPoints: () => search("points", {
            storeId: currentStoreId(),
            userId: document.getElementById("pointsUserId").value,
            type: document.getElementById("pointsType").value.trim(),
            status: document.getElementById("pointsStatus").value
        }),
        searchAccounts: () => search("accounts", {
            storeId: currentStoreId(),
            userId: document.getElementById("accountUserId").value,
            type: document.getElementById("accountType").value.trim(),
            bizNo: document.getElementById("accountBizNo").value.trim()
        }),
        searchOperations: () => search("operations", {
            adminId: document.getElementById("operationAdminId").value,
            module: document.getElementById("operationModule").value.trim(),
            startTime: document.getElementById("operationStart").value,
            endTime: document.getElementById("operationEnd").value
        })
    };

    if (state.token && state.profile) {
        enterApp();
    }
})();
