const STORAGE_KEY = "seed-manager-data-v1";

const state = {
  categories: [],
  products: [],
  variants: []
};

const refs = {
  statsPanel: document.getElementById("statsPanel"),

  categoryForm: document.getElementById("categoryForm"),
  categoryId: document.getElementById("categoryId"),
  categoryName: document.getElementById("categoryName"),
  categorySlug: document.getElementById("categorySlug"),
  categoryParent: document.getElementById("categoryParent"),
  categoryTable: document.getElementById("categoryTable"),
  categoryReset: document.getElementById("categoryReset"),

  productForm: document.getElementById("productForm"),
  productId: document.getElementById("productId"),
  productName: document.getElementById("productName"),
  productSlug: document.getElementById("productSlug"),
  productCategory: document.getElementById("productCategory"),
  productBrand: document.getElementById("productBrand"),
  productPrice: document.getElementById("productPrice"),
  productActive: document.getElementById("productActive"),
  productTable: document.getElementById("productTable"),
  productReset: document.getElementById("productReset"),

  variantForm: document.getElementById("variantForm"),
  variantId: document.getElementById("variantId"),
  variantProduct: document.getElementById("variantProduct"),
  variantSku: document.getElementById("variantSku"),
  variantSize: document.getElementById("variantSize"),
  variantColor: document.getElementById("variantColor"),
  variantStock: document.getElementById("variantStock"),
  variantPrice: document.getElementById("variantPrice"),
  variantTable: document.getElementById("variantTable"),
  variantFilter: document.getElementById("variantFilter"),
  variantReset: document.getElementById("variantReset"),

  apiBaseUrl: document.getElementById("apiBaseUrl"),
  apiToken: document.getElementById("apiToken"),
  apiLog: document.getElementById("apiLog"),

  btnGenerate: document.getElementById("btnGenerate"),
  btnPushApi: document.getElementById("btnPushApi"),
  btnExport: document.getElementById("btnExport"),
  fileImport: document.getElementById("fileImport"),
  btnClear: document.getElementById("btnClear")
};

function uid(prefix) {
  return `${prefix}_${Math.random().toString(36).slice(2, 8)}_${Date.now().toString(36)}`;
}

function formatPrice(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  return Number(value).toLocaleString("vi-VN") + " đ";
}

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function logApi(message) {
  const time = new Date().toLocaleTimeString("vi-VN");
  if (!refs.apiLog) {
    return;
  }
  refs.apiLog.textContent += `\n[${time}] ${message}`;
  refs.apiLog.scrollTop = refs.apiLog.scrollHeight;
}

function resetApiLog(message = "Ready.") {
  if (refs.apiLog) {
    refs.apiLog.textContent = message;
  }
}

function normalizeApiBase() {
  const base = (refs.apiBaseUrl?.value || "/api").trim().replace(/\/$/, "");
  return base || "/api";
}

function buildHeaders() {
  const headers = {
    "Content-Type": "application/json"
  };

  const token = (refs.apiToken?.value || "").trim();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

async function apiCall(path, method, body) {
  const base = normalizeApiBase();
  const url = `${base}${path}`;
  const response = await fetch(url, {
    method,
    credentials: "include",
    headers: buildHeaders(),
    body: body ? JSON.stringify(body) : undefined
  });

  let payload = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }

  if (!response.ok) {
    const msg = payload?.message || payload?.error || `HTTP ${response.status}`;
    throw new Error(`${method} ${path} failed: ${msg}`);
  }

  return payload;
}

function getData(payload) {
  return payload?.data ?? payload;
}

function slugify(text) {
  return String(text || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "");
}

async function ensureBrandsByName(brandNames) {
  const index = new Map();
  const brandListPayload = await apiCall("/brands", "GET");
  const brands = Array.isArray(getData(brandListPayload)) ? getData(brandListPayload) : [];

  for (const b of brands) {
    index.set(String(b.name || "").toLowerCase(), b.id);
  }

  for (const name of brandNames) {
    const key = String(name || "").trim().toLowerCase();
    if (!key) {
      continue;
    }
    if (index.has(key)) {
      continue;
    }

    const createdPayload = await apiCall("/brands", "POST", {
      name,
      description: "Seeded from seed-manager"
    });
    const created = getData(createdPayload);
    index.set(key, created.id);
    logApi(`Brand created: ${name} -> id=${created.id}`);
  }

  return index;
}

async function pushAllToApi() {
  if (!state.categories.length || !state.products.length) {
    alert("Cần có ít nhất 1 category và 1 product để push API.");
    return;
  }

  resetApiLog("Starting push...");
  logApi(`Categories: ${state.categories.length}, Products: ${state.products.length}, Variants: ${state.variants.length}`);

  const localCategoryToServerId = new Map();
  const localProductToServerId = new Map();

  try {
    const brandsNeeded = [...new Set(state.products.map(p => p.brand).filter(Boolean))];
    logApi(`Ensuring brands (${brandsNeeded.length})...`);
    const brandMap = await ensureBrandsByName(brandsNeeded);

    logApi("Creating categories...");
    const remaining = [...state.categories];
    let guard = 0;
    while (remaining.length && guard < 20) {
      guard += 1;
      let progressed = false;

      for (let i = remaining.length - 1; i >= 0; i -= 1) {
        const c = remaining[i];
        if (c.parentId && !localCategoryToServerId.has(c.parentId)) {
          continue;
        }

        const payload = {
          name: c.name,
          description: "Seeded from seed-manager",
          parentId: c.parentId ? localCategoryToServerId.get(c.parentId) : null
        };

        const createdPayload = await apiCall("/categories", "POST", payload);
        const created = getData(createdPayload);
        localCategoryToServerId.set(c.id, created.id);
        logApi(`Category created: ${c.name} -> id=${created.id}`);
        remaining.splice(i, 1);
        progressed = true;
      }

      if (!progressed && remaining.length) {
        throw new Error("Không thể map parent category. Kiểm tra dữ liệu category.");
      }
    }

    logApi("Creating products...");
    for (const p of state.products) {
      const categoryId = localCategoryToServerId.get(p.categoryId);
      const brandId = brandMap.get(String(p.brand || "").trim().toLowerCase());

      if (!categoryId) {
        throw new Error(`Product ${p.name} thiếu category mapping.`);
      }
      if (!brandId) {
        throw new Error(`Product ${p.name} thiếu brand mapping.`);
      }

      const createdPayload = await apiCall("/products", "POST", {
        name: p.name,
        shortDescription: "Seeded from seed-manager",
        description: "Seeded from seed-manager",
        basePrice: Number(p.basePrice || 0),
        categoryId,
        brandId
      });
      const created = getData(createdPayload);
      localProductToServerId.set(p.id, created.id);
      logApi(`Product created: ${p.name} -> id=${created.id}`);
    }

    logApi("Creating variants...");
    for (const v of state.variants) {
      const productId = localProductToServerId.get(v.productId);
      if (!productId) {
        logApi(`Skip variant ${v.sku}: product mapping not found`);
        continue;
      }

      const variantPayload = {
        sku: v.sku,
        weight: null,
        gripSize: null,
        stiffness: null,
        balancePoint: null,
        size: v.size,
        color: v.color,
        price: Number(v.price ?? state.products.find(p => p.id === v.productId)?.basePrice ?? 1),
        stock: Number(v.stock ?? 0),
        shippingWeightGrams: 300,
        shippingLengthCm: 70,
        shippingWidthCm: 25,
        shippingHeightCm: 5
      };

      await apiCall(`/products/${productId}/variants`, "POST", variantPayload);
      logApi(`Variant created: ${v.sku} -> productId=${productId}`);
    }

    logApi("Push completed successfully.");
    alert("Push to API thành công.");
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    logApi(`ERROR: ${message}`);
    alert(`Push to API thất bại: ${message}`);
  }
}

function load() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return;
  }
  try {
    const parsed = JSON.parse(raw);
    state.categories = Array.isArray(parsed.categories) ? parsed.categories : [];
    state.products = Array.isArray(parsed.products) ? parsed.products : [];
    state.variants = Array.isArray(parsed.variants) ? parsed.variants : [];
  } catch {
    localStorage.removeItem(STORAGE_KEY);
  }
}

function escapeHtml(input) {
  return String(input)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function renderStats() {
  const activeProducts = state.products.filter(p => p.isActive).length;
  const totalStock = state.variants.reduce((sum, v) => sum + Number(v.stock || 0), 0);
  refs.statsPanel.innerHTML = `
    <div class="stat-box"><p>Categories</p><h3>${state.categories.length}</h3></div>
    <div class="stat-box"><p>Products</p><h3>${state.products.length}</h3></div>
    <div class="stat-box"><p>Variants</p><h3>${state.variants.length}</h3></div>
    <div class="stat-box"><p>Active Products</p><h3>${activeProducts}</h3></div>
    <div class="stat-box"><p>Total Stock</p><h3>${totalStock}</h3></div>
  `;
}

function fillCategorySelects() {
  const parentOptions = ["<option value=''>Không có parent</option>"];
  const categoryOptions = ["<option value=''>-- Chọn category --</option>"];

  for (const c of state.categories) {
    parentOptions.push(`<option value="${c.id}">${escapeHtml(c.name)}</option>`);
    categoryOptions.push(`<option value="${c.id}">${escapeHtml(c.name)}</option>`);
  }

  refs.categoryParent.innerHTML = parentOptions.join("");
  refs.productCategory.innerHTML = categoryOptions.join("");
}

function fillVariantProductSelect() {
  const options = ["<option value=''>-- Chọn product --</option>"];
  for (const p of state.products) {
    options.push(`<option value="${p.id}">${escapeHtml(p.name)}</option>`);
  }
  refs.variantProduct.innerHTML = options.join("");
}

function getCategoryName(id) {
  return state.categories.find(c => c.id === id)?.name || "-";
}

function getProductName(id) {
  return state.products.find(p => p.id === id)?.name || "-";
}

function renderCategories() {
  refs.categoryTable.innerHTML = state.categories.map(c => {
    const parent = getCategoryName(c.parentId);
    return `
      <tr>
        <td>${c.id}</td>
        <td>${escapeHtml(c.name)}</td>
        <td>${escapeHtml(c.slug)}</td>
        <td>${escapeHtml(parent)}</td>
        <td>
          <div class="table-actions">
            <button class="btn btn-mini" data-action="edit" data-id="${c.id}" data-type="category">Sửa</button>
            <button class="btn btn-mini btn-danger" data-action="delete" data-id="${c.id}" data-type="category">Xóa</button>
          </div>
        </td>
      </tr>
    `;
  }).join("");
}

function renderProducts() {
  refs.productTable.innerHTML = state.products.map(p => {
    const activeClass = p.isActive ? "on" : "off";
    const activeText = p.isActive ? "Active" : "Inactive";
    return `
      <tr>
        <td>${p.id}</td>
        <td>${escapeHtml(p.name)}</td>
        <td>${escapeHtml(getCategoryName(p.categoryId))}</td>
        <td>${escapeHtml(p.brand)}</td>
        <td>${formatPrice(p.basePrice)}</td>
        <td><span class="badge ${activeClass}">${activeText}</span></td>
        <td>
          <div class="table-actions">
            <button class="btn btn-mini" data-action="edit" data-id="${p.id}" data-type="product">Sửa</button>
            <button class="btn btn-mini btn-danger" data-action="delete" data-id="${p.id}" data-type="product">Xóa</button>
          </div>
        </td>
      </tr>
    `;
  }).join("");
}

function renderVariants() {
  const q = refs.variantFilter.value.trim().toLowerCase();
  refs.variantTable.innerHTML = state.variants
    .filter(v => {
      if (!q) {
        return true;
      }
      const productName = getProductName(v.productId).toLowerCase();
      return productName.includes(q) || String(v.sku).toLowerCase().includes(q);
    })
    .map(v => `
      <tr>
        <td>${v.id}</td>
        <td>${escapeHtml(getProductName(v.productId))}</td>
        <td>${escapeHtml(v.sku)}</td>
        <td>${escapeHtml(v.size)}</td>
        <td>${escapeHtml(v.color)}</td>
        <td>${v.stock}</td>
        <td>${formatPrice(v.price)}</td>
        <td>
          <div class="table-actions">
            <button class="btn btn-mini" data-action="edit" data-id="${v.id}" data-type="variant">Sửa</button>
            <button class="btn btn-mini btn-danger" data-action="delete" data-id="${v.id}" data-type="variant">Xóa</button>
          </div>
        </td>
      </tr>
    `).join("");
}

function rerenderAll() {
  fillCategorySelects();
  fillVariantProductSelect();
  renderStats();
  renderCategories();
  renderProducts();
  renderVariants();
  persist();
}

function resetCategoryForm() {
  refs.categoryForm.reset();
  refs.categoryId.value = "";
}

function resetProductForm() {
  refs.productForm.reset();
  refs.productId.value = "";
  refs.productActive.value = "true";
}

function resetVariantForm() {
  refs.variantForm.reset();
  refs.variantId.value = "";
}

function assertUnique(items, field, value, editingId) {
  return !items.some(i => i[field] === value && i.id !== editingId);
}

refs.categoryForm.addEventListener("submit", (e) => {
  e.preventDefault();
  const editingId = refs.categoryId.value;
  const payload = {
    id: editingId || uid("cat"),
    name: refs.categoryName.value.trim(),
    slug: refs.categorySlug.value.trim(),
    parentId: refs.categoryParent.value || null
  };

  if (!assertUnique(state.categories, "slug", payload.slug, editingId)) {
    alert("Slug category đã tồn tại.");
    return;
  }

  if (payload.parentId && payload.parentId === payload.id) {
    alert("Category không thể làm parent của chính nó.");
    return;
  }

  if (!editingId) {
    state.categories.push(payload);
  } else {
    const idx = state.categories.findIndex(c => c.id === editingId);
    if (idx >= 0) {
      state.categories[idx] = payload;
    }
  }

  rerenderAll();
  resetCategoryForm();
});

refs.productForm.addEventListener("submit", (e) => {
  e.preventDefault();

  if (!refs.productCategory.value) {
    alert("Bạn cần tạo category trước khi tạo product.");
    return;
  }

  const editingId = refs.productId.value;
  const payload = {
    id: editingId || uid("prod"),
    name: refs.productName.value.trim(),
    slug: refs.productSlug.value.trim(),
    categoryId: refs.productCategory.value,
    brand: refs.productBrand.value.trim(),
    basePrice: Number(refs.productPrice.value),
    isActive: refs.productActive.value === "true"
  };

  if (!assertUnique(state.products, "slug", payload.slug, editingId)) {
    alert("Slug product đã tồn tại.");
    return;
  }

  if (!editingId) {
    state.products.push(payload);
  } else {
    const idx = state.products.findIndex(p => p.id === editingId);
    if (idx >= 0) {
      state.products[idx] = payload;
    }
  }

  rerenderAll();
  resetProductForm();
});

refs.variantForm.addEventListener("submit", (e) => {
  e.preventDefault();

  if (!refs.variantProduct.value) {
    alert("Bạn cần tạo product trước khi tạo variant.");
    return;
  }

  const editingId = refs.variantId.value;
  const payload = {
    id: editingId || uid("var"),
    productId: refs.variantProduct.value,
    sku: refs.variantSku.value.trim(),
    size: refs.variantSize.value.trim(),
    color: refs.variantColor.value.trim(),
    stock: Number(refs.variantStock.value),
    price: refs.variantPrice.value ? Number(refs.variantPrice.value) : null
  };

  if (!assertUnique(state.variants, "sku", payload.sku, editingId)) {
    alert("SKU đã tồn tại.");
    return;
  }

  if (!editingId) {
    state.variants.push(payload);
  } else {
    const idx = state.variants.findIndex(v => v.id === editingId);
    if (idx >= 0) {
      state.variants[idx] = payload;
    }
  }

  rerenderAll();
  resetVariantForm();
});

document.body.addEventListener("click", (e) => {
  const target = e.target;
  if (!(target instanceof HTMLElement)) {
    return;
  }

  const action = target.dataset.action;
  const type = target.dataset.type;
  const id = target.dataset.id;

  if (!action || !type || !id) {
    return;
  }

  if (action === "edit") {
    if (type === "category") {
      const c = state.categories.find(x => x.id === id);
      if (!c) return;
      refs.categoryId.value = c.id;
      refs.categoryName.value = c.name;
      refs.categorySlug.value = c.slug;
      refs.categoryParent.value = c.parentId || "";
      window.scrollTo({ top: 0, behavior: "smooth" });
    }

    if (type === "product") {
      const p = state.products.find(x => x.id === id);
      if (!p) return;
      refs.productId.value = p.id;
      refs.productName.value = p.name;
      refs.productSlug.value = p.slug;
      refs.productCategory.value = p.categoryId;
      refs.productBrand.value = p.brand;
      refs.productPrice.value = p.basePrice;
      refs.productActive.value = String(p.isActive);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }

    if (type === "variant") {
      const v = state.variants.find(x => x.id === id);
      if (!v) return;
      refs.variantId.value = v.id;
      refs.variantProduct.value = v.productId;
      refs.variantSku.value = v.sku;
      refs.variantSize.value = v.size;
      refs.variantColor.value = v.color;
      refs.variantStock.value = v.stock;
      refs.variantPrice.value = v.price ?? "";
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  }

  if (action === "delete") {
    if (!confirm("Bạn chắc chắn muốn xóa bản ghi này?")) {
      return;
    }

    if (type === "category") {
      const categoryId = id;
      state.categories = state.categories.filter(c => c.id !== categoryId);
      state.products = state.products.filter(p => p.categoryId !== categoryId);
      const existingProductIds = new Set(state.products.map(p => p.id));
      state.variants = state.variants.filter(v => existingProductIds.has(v.productId));
    }

    if (type === "product") {
      state.products = state.products.filter(p => p.id !== id);
      state.variants = state.variants.filter(v => v.productId !== id);
    }

    if (type === "variant") {
      state.variants = state.variants.filter(v => v.id !== id);
    }

    rerenderAll();
  }
});

refs.variantFilter.addEventListener("input", renderVariants);
refs.categoryReset.addEventListener("click", resetCategoryForm);
refs.productReset.addEventListener("click", resetProductForm);
refs.variantReset.addEventListener("click", resetVariantForm);

refs.btnClear.addEventListener("click", () => {
  if (!confirm("Xóa toàn bộ dữ liệu trong localStorage?")) {
    return;
  }
  state.categories = [];
  state.products = [];
  state.variants = [];
  rerenderAll();
  resetCategoryForm();
  resetProductForm();
  resetVariantForm();
});

refs.btnExport.addEventListener("click", () => {
  const payload = JSON.stringify(state, null, 2);
  const blob = new Blob([payload], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `seed-data-${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
});

refs.fileImport.addEventListener("change", async (e) => {
  const input = e.target;
  if (!(input instanceof HTMLInputElement) || !input.files?.length) {
    return;
  }

  const file = input.files[0];
  try {
    const text = await file.text();
    const parsed = JSON.parse(text);

    state.categories = Array.isArray(parsed.categories) ? parsed.categories : [];
    state.products = Array.isArray(parsed.products) ? parsed.products : [];
    state.variants = Array.isArray(parsed.variants) ? parsed.variants : [];

    rerenderAll();
    resetCategoryForm();
    resetProductForm();
    resetVariantForm();
  } catch {
    alert("File JSON không hợp lệ.");
  }

  input.value = "";
});

refs.btnGenerate.addEventListener("click", () => {
  if (!confirm("Generate sample data sẽ ghi đè dữ liệu hiện tại. Tiếp tục?")) {
    return;
  }

  const catRacket = { id: uid("cat"), name: "Vợt cầu lông", slug: "vot-cau-long", parentId: null };
  const catShoes = { id: uid("cat"), name: "Giày cầu lông", slug: "giay-cau-long", parentId: null };
  const catString = { id: uid("cat"), name: "Dây đan vợt", slug: "day-dan-vot", parentId: null };

  const prod1 = {
    id: uid("prod"),
    name: "Yonex Astrox 100ZZ",
    slug: "yonex-astrox-100zz",
    categoryId: catRacket.id,
    brand: "Yonex",
    basePrice: 4650000,
    isActive: true
  };

  const prod2 = {
    id: uid("prod"),
    name: "Lining Axforce 90 Tiger",
    slug: "lining-axforce-90-tiger",
    categoryId: catRacket.id,
    brand: "Lining",
    basePrice: 3890000,
    isActive: true
  };

  const prod3 = {
    id: uid("prod"),
    name: "Victor A970 NitroLite",
    slug: "victor-a970-nitrolite",
    categoryId: catShoes.id,
    brand: "Victor",
    basePrice: 2650000,
    isActive: true
  };

  state.categories = [catRacket, catShoes, catString];
  state.products = [prod1, prod2, prod3];
  state.variants = [
    { id: uid("var"), productId: prod1.id, sku: "YONEX-100ZZ-4UG5", size: "4U/G5", color: "Kurenai", stock: 18, price: 4750000 },
    { id: uid("var"), productId: prod1.id, sku: "YONEX-100ZZ-3UG5", size: "3U/G5", color: "Kurenai", stock: 8, price: 4790000 },
    { id: uid("var"), productId: prod2.id, sku: "LINING-AF90-4UG6", size: "4U/G6", color: "Gold", stock: 22, price: 3920000 },
    { id: uid("var"), productId: prod3.id, sku: "VICTOR-A970-42", size: "42", color: "White/Red", stock: 14, price: null }
  ];

  rerenderAll();
  resetCategoryForm();
  resetProductForm();
  resetVariantForm();
});

refs.btnPushApi.addEventListener("click", () => {
  if (!confirm("Push toàn bộ dữ liệu local lên backend API?")) {
    return;
  }
  pushAllToApi();
});

load();
rerenderAll();
resetProductForm();
resetApiLog();
