package com.cloudware.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder {
    private final JdbcTemplate jdbc;

    public DataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void seed() {
        // backend-a and backend-b start at the same time. This PostgreSQL transaction lock
        // prevents both containers from creating/seeding the same schema concurrently.
        jdbc.execute("SELECT pg_advisory_xact_lock(2026060801)");
        createSchema();
        seedRolesAndPermissions();
        seedUsers();
        seedSettings();
        seedCategoriesProductsCustomersWarehouses();
        seedInventory();
        seedOrdersPaymentsActivityNotifications();
    }

    private void createSchema() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS roles (
                id BIGSERIAL PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                created_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS permissions (
                id BIGSERIAL PRIMARY KEY,
                code TEXT NOT NULL UNIQUE,
                description TEXT
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS role_permissions (
                role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
                permission_id BIGINT REFERENCES permissions(id) ON DELETE CASCADE,
                PRIMARY KEY(role_id, permission_id)
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS app_users (
                id BIGSERIAL PRIMARY KEY,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'VIEWER',
                department TEXT NOT NULL DEFAULT 'General',
                phone TEXT,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMPTZ DEFAULT NOW(),
                updated_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS user_roles (
                user_id BIGINT REFERENCES app_users(id) ON DELETE CASCADE,
                role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
                PRIMARY KEY(user_id, role_id)
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS auth_tokens (
                token TEXT PRIMARY KEY,
                user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                created_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS product_categories (
                id BIGSERIAL PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMPTZ DEFAULT NOW(),
                updated_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id BIGSERIAL PRIMARY KEY,
                sku TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                description TEXT,
                category_id BIGINT REFERENCES product_categories(id),
                category TEXT,
                brand TEXT,
                size_range TEXT,
                color TEXT,
                season TEXT,
                wholesale_price NUMERIC(12,2) NOT NULL DEFAULT 0,
                retail_price NUMERIC(12,2) NOT NULL DEFAULT 0,
                minimum_stock INT NOT NULL DEFAULT 0,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMPTZ DEFAULT NOW(),
                updated_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS customers (
                id BIGSERIAL PRIMARY KEY,
                company_name TEXT NOT NULL,
                contact_person TEXT,
                email TEXT,
                phone TEXT,
                city TEXT,
                address TEXT,
                segment TEXT,
                credit_limit NUMERIC(12,2) NOT NULL DEFAULT 0,
                current_debt NUMERIC(12,2) NOT NULL DEFAULT 0,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMPTZ DEFAULT NOW(),
                updated_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS warehouses (
                id BIGSERIAL PRIMARY KEY,
                name TEXT NOT NULL,
                code TEXT NOT NULL UNIQUE,
                city TEXT,
                address TEXT,
                capacity_units INT NOT NULL DEFAULT 0,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMPTZ DEFAULT NOW(),
                updated_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS inventory (
                id BIGSERIAL PRIMARY KEY,
                product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                warehouse_id BIGINT NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
                quantity INT NOT NULL DEFAULT 0,
                reserved_quantity INT NOT NULL DEFAULT 0,
                created_at TIMESTAMPTZ DEFAULT NOW(),
                updated_at TIMESTAMPTZ DEFAULT NOW(),
                UNIQUE(product_id, warehouse_id)
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS inventory_movements (
                id BIGSERIAL PRIMARY KEY,
                product_id BIGINT NOT NULL REFERENCES products(id),
                warehouse_id BIGINT REFERENCES warehouses(id),
                from_warehouse_id BIGINT REFERENCES warehouses(id),
                to_warehouse_id BIGINT REFERENCES warehouses(id),
                type TEXT NOT NULL,
                quantity INT NOT NULL,
                reason TEXT,
                created_by TEXT,
                created_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS orders (
                id BIGSERIAL PRIMARY KEY,
                order_number TEXT NOT NULL UNIQUE,
                customer_id BIGINT REFERENCES customers(id),
                customer_name TEXT,
                status TEXT NOT NULL DEFAULT 'DRAFT',
                priority TEXT NOT NULL DEFAULT 'NORMAL',
                delivery_city TEXT,
                delivery_address TEXT,
                subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
                discount NUMERIC(12,2) NOT NULL DEFAULT 0,
                tax NUMERIC(12,2) NOT NULL DEFAULT 0,
                delivery_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
                total_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                notes TEXT,
                created_at TIMESTAMPTZ DEFAULT NOW(),
                updated_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS order_items (
                id BIGSERIAL PRIMARY KEY,
                order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                product_id BIGINT NOT NULL REFERENCES products(id),
                product_name TEXT NOT NULL,
                sku TEXT NOT NULL,
                quantity INT NOT NULL,
                unit_price NUMERIC(12,2) NOT NULL,
                total_price NUMERIC(12,2) NOT NULL,
                UNIQUE(order_id, product_id)
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS payments (
                id BIGSERIAL PRIMARY KEY,
                order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
                customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
                amount NUMERIC(12,2) NOT NULL,
                method TEXT NOT NULL,
                status TEXT NOT NULL,
                payment_date DATE DEFAULT CURRENT_DATE,
                notes TEXT,
                created_at TIMESTAMPTZ DEFAULT NOW(),
                updated_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS app_settings (
                setting_key TEXT PRIMARY KEY,
                setting_group TEXT NOT NULL,
                setting_value TEXT NOT NULL,
                value_type TEXT NOT NULL DEFAULT 'string',
                updated_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS activity_log (
                id BIGSERIAL PRIMARY KEY,
                user_id BIGINT,
                user_name TEXT,
                module TEXT NOT NULL,
                action TEXT NOT NULL,
                description TEXT,
                ip_address TEXT,
                created_at TIMESTAMPTZ DEFAULT NOW()
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS notifications (
                id BIGSERIAL PRIMARY KEY,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                is_read BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMPTZ DEFAULT NOW()
            )""");

        // Compatibility with the original uploaded demo schema.
        addColumn("app_users", "phone", "TEXT");
        addColumn("app_users", "active", "BOOLEAN NOT NULL DEFAULT TRUE");
        addColumn("app_users", "created_at", "TIMESTAMPTZ DEFAULT NOW()");
        addColumn("app_users", "updated_at", "TIMESTAMPTZ DEFAULT NOW()");
        addColumn("products", "description", "TEXT");
        addColumn("products", "category_id", "BIGINT");
        addColumn("products", "brand", "TEXT");
        addColumn("products", "color", "TEXT");
        addColumn("products", "retail_price", "NUMERIC(12,2) NOT NULL DEFAULT 0");
        addColumn("products", "active", "BOOLEAN NOT NULL DEFAULT TRUE");
        addColumn("products", "created_at", "TIMESTAMPTZ DEFAULT NOW()");
        addColumn("products", "updated_at", "TIMESTAMPTZ DEFAULT NOW()");
        addColumn("customers", "address", "TEXT");
        addColumn("customers", "current_debt", "NUMERIC(12,2) NOT NULL DEFAULT 0");
        addColumn("customers", "active", "BOOLEAN NOT NULL DEFAULT TRUE");
        addColumn("customers", "created_at", "TIMESTAMPTZ DEFAULT NOW()");
        addColumn("customers", "updated_at", "TIMESTAMPTZ DEFAULT NOW()");
        addColumn("warehouses", "created_at", "TIMESTAMPTZ DEFAULT NOW()");
        addColumn("warehouses", "updated_at", "TIMESTAMPTZ DEFAULT NOW()");
        addColumn("orders", "customer_name", "TEXT");
        addColumn("orders", "delivery_address", "TEXT");
        addColumn("orders", "subtotal", "NUMERIC(12,2) NOT NULL DEFAULT 0");
        addColumn("orders", "discount", "NUMERIC(12,2) NOT NULL DEFAULT 0");
        addColumn("orders", "tax", "NUMERIC(12,2) NOT NULL DEFAULT 0");
        addColumn("orders", "delivery_fee", "NUMERIC(12,2) NOT NULL DEFAULT 0");
        addColumn("orders", "notes", "TEXT");
        addColumn("orders", "created_at", "TIMESTAMPTZ DEFAULT NOW()");
        addColumn("orders", "updated_at", "TIMESTAMPTZ DEFAULT NOW()");

        // Compatibility indexes for users who already have an older Docker volume.
        // Some old tables existed without UNIQUE constraints, but seed SQL uses ON CONFLICT(...).
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_warehouses_code ON warehouses(code)");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_order_number ON orders(order_number)");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_products_sku ON products(sku)");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_product_categories_name ON product_categories(name)");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_app_users_email ON app_users(email)");
    }

    private void addColumn(String table, String column, String definition) {
        jdbc.execute("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column + " " + definition);
    }

    private void seedRolesAndPermissions() {
        List<String> roles = List.of("ADMIN", "SELLER", "MANAGER", "WAREHOUSE_MANAGER", "ACCOUNTANT", "VIEWER");
        for (String role : roles) {
            jdbc.update("INSERT INTO roles(name,description) VALUES (?,?) ON CONFLICT(name) DO NOTHING", role, role.replace('_', ' ') + " role");
        }
        List<String> permissions = List.of(
            "products.read", "products.write", "customers.read", "customers.write", "orders.read", "orders.write",
            "inventory.read", "inventory.write", "reports.read", "settings.write", "users.manage"
        );
        for (String p : permissions) {
            jdbc.update("INSERT INTO permissions(code,description) VALUES (?,?) ON CONFLICT(code) DO NOTHING", p, "Allows " + p);
        }
        grant("ADMIN", permissions);
        grant("SELLER", List.of("products.read", "products.write", "customers.read", "customers.write", "orders.read", "orders.write"));
        grant("MANAGER", List.of("products.read", "customers.read", "orders.read", "orders.write", "inventory.read", "reports.read"));
        grant("WAREHOUSE_MANAGER", List.of("products.read", "orders.read", "inventory.read", "inventory.write"));
        grant("ACCOUNTANT", List.of("customers.read", "orders.read", "reports.read"));
        grant("VIEWER", List.of("products.read", "customers.read", "orders.read", "inventory.read", "reports.read"));
    }

    private void grant(String role, List<String> permissions) {
        for (String permission : permissions) {
            jdbc.update("""
                INSERT INTO role_permissions(role_id, permission_id)
                SELECT r.id, p.id FROM roles r, permissions p WHERE r.name=? AND p.code=?
                ON CONFLICT DO NOTHING
                """, role, permission);
        }
    }

    private void seedUsers() {
        insertUser("System Admin", "admin@cloudware.local", "admin123", "ADMIN", "IT", "+998 90 111 22 33");
        insertUser("Dilshod Seller", "seller@cloudware.local", "seller123", "SELLER", "Sales", "+998 91 222 33 44");
        insertUser("Aziz Warehouse", "warehouse@cloudware.local", "warehouse123", "WAREHOUSE_MANAGER", "Warehouse", "+998 93 333 44 55");
        insertUser("Madina Accountant", "accountant@cloudware.local", "accountant123", "ACCOUNTANT", "Finance", "+998 94 444 55 66");
        insertUser("Viewer User", "viewer@cloudware.local", "viewer123", "VIEWER", "Management", "+998 95 555 66 77");
        jdbc.update("""
            INSERT INTO user_roles(user_id, role_id)
            SELECT u.id, r.id FROM app_users u JOIN roles r ON r.name = u.role
            ON CONFLICT DO NOTHING
            """);
    }

    private void insertUser(String fullName, String email, String password, String role, String department, String phone) {
        jdbc.update("""
            INSERT INTO app_users(full_name,email,password,role,department,phone,active)
            VALUES (?,?,?,?,?,?,TRUE)
            ON CONFLICT(email) DO UPDATE SET full_name=EXCLUDED.full_name, role=EXCLUDED.role, department=EXCLUDED.department, phone=EXCLUDED.phone
            """, fullName, email, password, role, department, phone);
    }

    private void seedSettings() {
        setting("company", "companyName", "CloudWare Pro Textile", "string");
        setting("company", "legalName", "CloudWare Pro Textile LLC", "string");
        setting("company", "taxNumber", "305123456", "string");
        setting("company", "phone", "+998 71 200 10 20", "string");
        setting("company", "email", "info@cloudware.local", "string");
        setting("company", "website", "https://cloudware.local", "string");
        setting("company", "address", "Yakkasaray district, Tashkent", "string");
        setting("company", "city", "Tashkent", "string");
        setting("company", "country", "Uzbekistan", "string");
        setting("store", "storeName", "CloudWare Wholesale Store", "string");
        setting("store", "defaultWarehouseId", "1", "number");
        setting("store", "defaultCustomerSegment", "Wholesale Partner", "string");
        setting("store", "workingHours", "09:00 - 18:00", "string");
        setting("store", "supportPhone", "+998 71 200 10 21", "string");
        setting("store", "supportEmail", "support@cloudware.local", "string");
        setting("tax", "taxEnabled", "true", "boolean");
        setting("tax", "taxPercent", "12", "number");
        setting("tax", "taxName", "VAT", "string");
        setting("currency", "currencyCode", "USD", "string");
        setting("currency", "currencySymbol", "$", "string");
        setting("currency", "exchangeRate", "12650", "number");
        setting("currency", "priceRoundingEnabled", "true", "boolean");
        setting("notifications", "emailNotifications", "true", "boolean");
        setting("notifications", "lowStockAlerts", "true", "boolean");
        setting("notifications", "orderStatusAlerts", "true", "boolean");
        setting("notifications", "paymentAlerts", "true", "boolean");
        setting("notifications", "dailyReportEnabled", "false", "boolean");
        setting("order", "autoGenerateOrderNumber", "true", "boolean");
        setting("order", "orderPrefix", "CW", "string");
        setting("order", "defaultOrderStatus", "DRAFT", "string");
        setting("order", "allowNegativeStock", "false", "boolean");
        setting("order", "reserveStockOnConfirm", "true", "boolean");
        setting("order", "autoMarkPaidAfterDelivery", "false", "boolean");
        setting("inventory", "lowStockThreshold", "100", "number");
        setting("inventory", "stockMovementRequiredReason", "true", "boolean");
        setting("inventory", "allowWarehouseTransfer", "true", "boolean");
        setting("inventory", "showOutOfStockProducts", "true", "boolean");
        setting("security", "sessionTimeoutMinutes", "120", "number");
        setting("security", "requireStrongPassword", "false", "boolean");
        setting("security", "allowMultipleSessions", "true", "boolean");
        setting("theme", "themeMode", "dark", "string");
        setting("theme", "sidebarCollapsed", "false", "boolean");
        setting("theme", "accentColor", "#38bdf8", "string");
        setting("theme", "compactMode", "false", "boolean");
    }

    private void setting(String group, String key, String value, String type) {
        jdbc.update("""
            INSERT INTO app_settings(setting_group,setting_key,setting_value,value_type)
            VALUES (?,?,?,?) ON CONFLICT(setting_key) DO NOTHING
            """, group, key, value, type);
    }

    private void seedCategoriesProductsCustomersWarehouses() {
        String[][] categories = {
            {"Hoodies", "Warm hoodies and sweatshirts"}, {"T-Shirts", "Cotton basics"}, {"Denim", "Jeans and denim goods"},
            {"Dresses", "Women dresses"}, {"Outerwear", "Jackets and coats"}, {"Kidswear", "Children clothing"}
        };
        for (String[] c : categories) jdbc.update("INSERT INTO product_categories(name,description) VALUES (?,?) ON CONFLICT(name) DO NOTHING", c[0], c[1]);

        Object[][] products = {
            {"CW-HDY-001", "Classic Oversize Hoodie", "Heavy cotton hoodie for wholesale stores", "Hoodies", "CloudWear", "S-XXL", "Black", "Winter", 18.50, 29.90, 120},
            {"CW-HDY-002", "Zip Hoodie Urban Line", "Zip hoodie with soft fleece", "Hoodies", "CloudWear", "S-XXL", "Grey", "Winter", 19.70, 32.00, 100},
            {"CW-TSH-101", "Premium Cotton T-Shirt", "180 GSM cotton basic t-shirt", "T-Shirts", "CottonBase", "XS-XXL", "White", "All season", 5.20, 10.00, 300},
            {"CW-TSH-102", "Relaxed Fit T-Shirt", "Relaxed streetwear t-shirt", "T-Shirts", "CottonBase", "S-XL", "Black", "All season", 5.90, 11.50, 280},
            {"CW-TSH-103", "Kids Basic T-Shirt", "Soft cotton shirt for kids", "Kidswear", "LittleWear", "2-12Y", "Mixed", "Summer", 3.80, 8.00, 200},
            {"CW-JNS-201", "Slim Fit Denim Jeans", "Classic slim fit denim", "Denim", "DenimPro", "28-38", "Blue", "All season", 21.00, 38.00, 90},
            {"CW-JNS-202", "Straight Denim Jeans", "Straight cut denim model", "Denim", "DenimPro", "28-40", "Dark Blue", "All season", 23.00, 41.00, 90},
            {"CW-DRS-301", "Summer Floral Dress", "Light summer dress", "Dresses", "ModaLine", "XS-L", "Floral", "Summer", 14.50, 26.00, 75},
            {"CW-DRS-302", "Business Midi Dress", "Office style midi dress", "Dresses", "ModaLine", "XS-XL", "Navy", "Spring", 17.00, 31.00, 70},
            {"CW-JKT-401", "Light Bomber Jacket", "Spring bomber jacket", "Outerwear", "StreetCore", "S-XXL", "Olive", "Spring", 33.00, 58.00, 50},
            {"CW-JKT-402", "Puffer Jacket", "Warm winter puffer", "Outerwear", "StreetCore", "S-XXL", "Black", "Winter", 42.00, 74.00, 45},
            {"CW-KID-501", "Kids Denim Jacket", "Denim jacket for kids", "Kidswear", "LittleWear", "4-12Y", "Blue", "Spring", 12.00, 23.00, 60},
            {"CW-TRS-601", "Women Wide Trousers", "Comfort wide trousers", "Dresses", "ModaLine", "XS-XL", "Beige", "All season", 13.80, 25.00, 80},
            {"CW-SRT-701", "Men Cotton Shorts", "Summer cotton shorts", "T-Shirts", "CottonBase", "S-XXL", "Khaki", "Summer", 7.80, 15.00, 130},
            {"CW-SET-801", "Tracksuit Set", "Two piece tracksuit set", "Hoodies", "CloudWear", "S-XXL", "Black", "Autumn", 27.00, 49.00, 85},
            {"CW-SET-802", "Kids Sport Set", "Kids sportwear set", "Kidswear", "LittleWear", "4-12Y", "Mixed", "All season", 10.20, 20.00, 100},
            {"CW-SHR-901", "Business Shirt", "Men formal business shirt", "T-Shirts", "CottonBase", "S-XXL", "White", "All season", 9.50, 18.50, 110},
            {"CW-SHR-902", "Oxford Shirt", "Oxford casual shirt", "T-Shirts", "CottonBase", "S-XXL", "Light Blue", "All season", 10.00, 19.00, 110},
            {"CW-COA-1001", "Wool Blend Coat", "Wool blend winter coat", "Outerwear", "StreetCore", "S-XL", "Camel", "Winter", 55.00, 98.00, 35},
            {"CW-ACC-1101", "Cotton Cap Pack", "Pack of cotton caps", "Outerwear", "StreetCore", "One size", "Mixed", "Summer", 2.90, 6.50, 180}
        };
        for (Object[] p : products) insertProduct(p);

        Object[][] customers = {
            {"Atlas Fashion Group", "Madina Karimova", "orders@atlas.local", "+998 90 100 20 30", "Tashkent", "Yakkasaray, Bobur street", "VIP Retail Chain", 50000, 8200},
            {"Samarkand Style Market", "Akmal Rakhimov", "sales@samstyle.local", "+998 91 400 55 66", "Samarkand", "Registan avenue", "Regional Distributor", 28000, 2300},
            {"Bukhara Textile Store", "Dilnoza Juraeva", "info@bukhara-textile.local", "+998 93 700 11 22", "Bukhara", "Old city market", "Wholesale Partner", 35000, 4100},
            {"Fergana Moda Center", "Sherzod Aliyev", "buy@fermoda.local", "+998 94 555 22 11", "Fergana", "Central bazaar", "Regional Distributor", 26000, 0},
            {"Andijan Kids World", "Nargiza Umurova", "kids@andijan.local", "+998 95 333 44 55", "Andijan", "Navoi street", "Kidswear Buyer", 18000, 1200},
            {"Namangan Retail Hub", "Jasur Tursunov", "orders@namhub.local", "+998 97 123 45 67", "Namangan", "Industrial zone", "Wholesale Partner", 22000, 3500},
            {"Qarshi Family Market", "Gulnoza Asadova", "contact@qarshi-market.local", "+998 88 777 66 55", "Qarshi", "Mustaqillik street", "Retail Chain", 17000, 760},
            {"Nukus Apparel Trade", "Azamat Berdiev", "trade@nukusapparel.local", "+998 99 909 80 70", "Nukus", "Dosnazarov street", "Regional Distributor", 24000, 1800},
            {"Termez Fashion Line", "Kamola Safarova", "line@termez.local", "+998 90 878 65 43", "Termez", "Alpomish street", "Wholesale Partner", 19000, 0},
            {"Jizzakh Uniform Shop", "Oybek Ismailov", "uniform@jizzakh.local", "+998 91 555 15 15", "Jizzakh", "Sharof Rashidov street", "Corporate Buyer", 15000, 2200}
        };
        for (Object[] c : customers) insertCustomer(c);

        Object[][] warehouses = {
            {"Tashkent Main Warehouse", "TSH-MAIN", "Tashkent", "Yakkasaray district logistics zone", 18000},
            {"Samarkand Regional Hub", "SAM-R01", "Samarkand", "Regional distribution centre", 9000},
            {"Fergana Fast Dispatch Hub", "FER-HUB", "Fergana", "Fast dispatch warehouse", 6500}
        };
        for (Object[] w : warehouses) jdbc.update("""
            INSERT INTO warehouses(name,code,city,address,capacity_units,active) VALUES (?,?,?,?,?,TRUE)
            ON CONFLICT(code) DO UPDATE SET name=EXCLUDED.name, city=EXCLUDED.city, address=EXCLUDED.address, capacity_units=EXCLUDED.capacity_units, active=TRUE
            """, w);
    }

    private void insertProduct(Object[] p) {
        Long categoryId = jdbc.queryForObject("SELECT id FROM product_categories WHERE name=?", Long.class, p[3]);
        jdbc.update("""
            INSERT INTO products(sku,name,description,category_id,category,brand,size_range,color,season,wholesale_price,retail_price,minimum_stock,active)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,TRUE)
            ON CONFLICT(sku) DO UPDATE SET name=EXCLUDED.name, description=EXCLUDED.description, category_id=EXCLUDED.category_id,
              category=EXCLUDED.category, brand=EXCLUDED.brand, size_range=EXCLUDED.size_range, color=EXCLUDED.color, season=EXCLUDED.season,
              wholesale_price=EXCLUDED.wholesale_price, retail_price=EXCLUDED.retail_price, minimum_stock=EXCLUDED.minimum_stock, active=TRUE
            """, p[0], p[1], p[2], categoryId, p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10]);
    }

    private void insertCustomer(Object[] c) {
        jdbc.update("""
            INSERT INTO customers(company_name,contact_person,email,phone,city,address,segment,credit_limit,current_debt,active)
            SELECT ?,?,?,?,?,?,?,?,?,TRUE
            WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email=?)
            """, c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[2]);
    }

    private void seedInventory() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM inventory", Long.class);
        if (count != null && count >= 30) return;
        List<Long> products = jdbc.queryForList("SELECT id FROM products ORDER BY id LIMIT 10", Long.class);
        List<Long> warehouses = jdbc.queryForList("SELECT id FROM warehouses ORDER BY id", Long.class);
        int seed = 0;
        for (Long productId : products) {
            for (Long warehouseId : warehouses) {
                int qty = 80 + ((seed * 37) % 520);
                int reserved = Math.max(0, qty / 12);
                jdbc.update("""
                    INSERT INTO inventory(product_id,warehouse_id,quantity,reserved_quantity)
                    VALUES (?,?,?,?) ON CONFLICT(product_id,warehouse_id) DO UPDATE SET quantity=EXCLUDED.quantity, reserved_quantity=EXCLUDED.reserved_quantity, updated_at=NOW()
                    """, productId, warehouseId, qty, reserved);
                seed++;
            }
        }
    }

    private void seedOrdersPaymentsActivityNotifications() {
        Long orderCount = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        if (orderCount == null || orderCount < 20) {
            String[] statuses = {"DRAFT", "NEW", "CONFIRMED", "PACKING", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED"};
            Long customerMax = jdbc.queryForObject("SELECT MAX(id) FROM customers", Long.class);
            Long productMax = jdbc.queryForObject("SELECT MAX(id) FROM products", Long.class);
            for (int i = 1; i <= 20; i++) {
                long customerId = ((i - 1) % customerMax) + 1;
                String customerName = jdbc.queryForObject("SELECT company_name FROM customers WHERE id=?", String.class, customerId);
                String status = statuses[i % statuses.length];
                String orderNumber = "CW-" + String.format("%05d", 10000 + i);
                BigDecimal discount = BigDecimal.valueOf((i % 3) * 15L);
                BigDecimal deliveryFee = BigDecimal.valueOf(20 + (i % 4) * 5L);
                jdbc.update("""
                    INSERT INTO orders(order_number,customer_id,customer_name,status,priority,delivery_city,delivery_address,discount,delivery_fee,notes)
                    VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT(order_number) DO NOTHING
                    """, orderNumber, customerId, customerName, status, i % 4 == 0 ? "HIGH" : "NORMAL", cityBy(i), cityBy(i) + " delivery address", discount, deliveryFee, "Seeded wholesale order");
                Long orderId = jdbc.queryForObject("SELECT id FROM orders WHERE order_number=?", Long.class, orderNumber);
                for (int j = 0; j < 2; j++) {
                    long productId = ((i + j) % productMax) + 1;
                    var product = jdbc.queryForMap("SELECT name, sku, wholesale_price FROM products WHERE id=?", productId);
                    int quantity = 15 + ((i + j) * 7) % 55;
                    BigDecimal unit = (BigDecimal) product.get("wholesale_price");
                    BigDecimal total = unit.multiply(BigDecimal.valueOf(quantity));
                    jdbc.update("""
                        INSERT INTO order_items(order_id,product_id,product_name,sku,quantity,unit_price,total_price)
                        VALUES (?,?,?,?,?,?,?) ON CONFLICT(order_id, product_id) DO NOTHING
                        """, orderId, productId, product.get("name"), product.get("sku"), quantity, unit, total);
                }
                recalcOrder(orderId);
            }
        }

        Long paymentCount = jdbc.queryForObject("SELECT COUNT(*) FROM payments", Long.class);
        if (paymentCount == null || paymentCount < 10) {
            List<Long> deliveredOrders = jdbc.queryForList("SELECT id FROM orders WHERE status IN ('DELIVERED','SHIPPED','CONFIRMED') ORDER BY id LIMIT 10", Long.class);
            String[] methods = {"CASH", "CARD", "BANK_TRANSFER", "PAYME", "CLICK", "UZUM_BANK"};
            int i = 0;
            for (Long orderId : deliveredOrders) {
                var order = jdbc.queryForMap("SELECT customer_id,total_amount FROM orders WHERE id=?", orderId);
                jdbc.update("""
                    INSERT INTO payments(order_id,customer_id,amount,method,status,payment_date,notes)
                    VALUES (?,?,?,?,?,CURRENT_DATE - (CAST(? AS TEXT) || ' days')::interval,?)
                    """, orderId, order.get("customer_id"), order.get("total_amount"), methods[i % methods.length], i % 4 == 0 ? "PARTIAL" : "PAID", i, "Seeded payment");
                i++;
            }
        }

        Long logCount = jdbc.queryForObject("SELECT COUNT(*) FROM activity_log", Long.class);
        if (logCount == null || logCount < 10) {
            log("System", "Deployment", "completed", "CloudWare Pro started with backend-a/backend-b and PostgreSQL");
            log("System", "Products", "seeded", "20 realistic clothing products created");
            log("System", "Inventory", "checked", "Low stock rules are active");
            log("System", "Orders", "workflow", "Order confirm, cancel, ship and deliver actions are enabled");
            log("System", "Settings", "configured", "Company, tax, currency and theme settings saved");
            log("System", "Users", "created", "Default roles and permissions were created");
            log("System", "Payments", "created", "Payment methods for Uzbekistan were added");
            log("System", "Reports", "ready", "Sales, revenue and inventory reports are available");
            log("System", "WMS", "transfer", "Warehouse transfer endpoint is ready");
            log("System", "Notifications", "ready", "Unread notification counter is active");
        }

        Long notificationCount = jdbc.queryForObject("SELECT COUNT(*) FROM notifications", Long.class);
        if (notificationCount == null || notificationCount < 5) {
            notify("LOW_STOCK", "Low stock warning", "Some items are close to minimum stock threshold");
            notify("NEW_ORDER", "New order received", "A new wholesale order is waiting for confirmation");
            notify("PAYMENT_PENDING", "Payment pending", "One customer has a pending payment");
            notify("ORDER_DELIVERED", "Order delivered", "A shipped order was marked as delivered");
            notify("WAREHOUSE_TRANSFER", "Warehouse transfer", "Inventory transfer between warehouses was recorded");
        }
    }

    private void recalcOrder(Long orderId) {
        BigDecimal subtotal = jdbc.queryForObject("SELECT COALESCE(SUM(total_price),0) FROM order_items WHERE order_id=?", BigDecimal.class, orderId);
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        var order = jdbc.queryForMap("SELECT discount,tax,delivery_fee FROM orders WHERE id=?", orderId);
        BigDecimal discount = (BigDecimal) order.get("discount");
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.12));
        BigDecimal delivery = (BigDecimal) order.get("delivery_fee");
        BigDecimal total = subtotal.subtract(discount == null ? BigDecimal.ZERO : discount).add(tax).add(delivery == null ? BigDecimal.ZERO : delivery);
        jdbc.update("UPDATE orders SET subtotal=?, tax=?, total_amount=?, updated_at=NOW() WHERE id=?", subtotal, tax, total, orderId);
    }

    private String cityBy(int i) {
        String[] cities = {"Tashkent", "Samarkand", "Bukhara", "Fergana", "Andijan", "Namangan", "Qarshi", "Nukus", "Termez", "Jizzakh"};
        return cities[i % cities.length];
    }

    private void log(String userName, String module, String action, String description) {
        jdbc.update("INSERT INTO activity_log(user_name,module,action,description,created_at) VALUES (?,?,?,?,NOW())", userName, module, action, description);
    }

    private void notify(String type, String title, String message) {
        jdbc.update("INSERT INTO notifications(type,title,message,is_read,created_at) VALUES (?,?,?,FALSE,NOW())", type, title, message);
    }
}
