-- ==========================================================
-- 1. ACCOUNT MANAGEMENT AND ROLE-BASED ACCESS CONTROL (RBAC)
-- ==========================================================

CREATE TABLE USERS (
    user_id      VARCHAR2(50) PRIMARY KEY,
    full_name    NVARCHAR2(255),
    email        VARCHAR2(150),
    phone_number VARCHAR2(20),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted   NUMBER(1) DEFAULT 0
);

CREATE TABLE FUNCTIONS (
    function_id   VARCHAR2(50) PRIMARY KEY,
    function_name NVARCHAR2(100),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted    NUMBER(1) DEFAULT 0
);

CREATE TABLE ROLE_GROUPS (
    role_group_id VARCHAR2(50) PRIMARY KEY,
    group_name    NVARCHAR2(100),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted    NUMBER(1) DEFAULT 0
);

CREATE TABLE ACCOUNTS (
    account_id         VARCHAR2(50) PRIMARY KEY,
    user_id            VARCHAR2(50)  NOT NULL,
    username           VARCHAR2(50)  NOT NULL,
    password           VARCHAR2(255) NOT NULL,
    status             NVARCHAR2(20),
    ACTIVE_SESSIONS    NUMBER(10) DEFAULT 0 NOT NULL,
    LAST_HEARTBEAT_AT  TIMESTAMP,
    CURRENT_SESSION_ID VARCHAR2(100),
    online_status      VARCHAR2(20),
    last_login_at      TIMESTAMP,
    last_logout_at     TIMESTAMP,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted         NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_ACCOUNTS_USERS FOREIGN KEY (user_id) REFERENCES USERS (user_id)
);

CREATE TABLE ROLES (
    role_id     VARCHAR2(50) PRIMARY KEY,
    role_name   NVARCHAR2(100),
    function_id VARCHAR2(50) NOT NULL,
    can_view    NUMBER(1) DEFAULT 0,
    can_add     NUMBER(1) DEFAULT 0,
    can_edit    NUMBER(1) DEFAULT 0,
    can_delete  NUMBER(1) DEFAULT 0,
    can_export  NUMBER(1) DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted  NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_ROLES_FUNCTIONS FOREIGN KEY (function_id) REFERENCES FUNCTIONS (function_id)
);

CREATE TABLE ACCOUNT_ASSIGN_ROLE_GROUP (
    account_id    VARCHAR2(50),
    role_group_id VARCHAR2(50),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted    NUMBER(1) DEFAULT 0,
    PRIMARY KEY (account_id, role_group_id),
    CONSTRAINT FK_AARG_ACCOUNTS FOREIGN KEY (account_id) REFERENCES ACCOUNTS (account_id),
    CONSTRAINT FK_AARG_ROLE_GROUPS FOREIGN KEY (role_group_id) REFERENCES ROLE_GROUPS (role_group_id)
);

CREATE TABLE ROLE_GROUP_ASSIGN_ROLE (
    role_group_id VARCHAR2(50),
    role_id       VARCHAR2(50),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted    NUMBER(1) DEFAULT 0,
    PRIMARY KEY (role_group_id, role_id),
    CONSTRAINT FK_RGAR_ROLE_GROUPS FOREIGN KEY (role_group_id) REFERENCES ROLE_GROUPS (role_group_id),
    CONSTRAINT FK_RGAR_ROLES FOREIGN KEY (role_id) REFERENCES ROLES (role_id)
);

CREATE TABLE ACCOUNT_ASSIGN_ROLE (
    account_id VARCHAR2(50),
    role_id    VARCHAR2(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0,
    PRIMARY KEY (account_id, role_id),
    CONSTRAINT FK_AAR_ACCOUNTS FOREIGN KEY (account_id) REFERENCES ACCOUNTS (account_id),
    CONSTRAINT FK_AAR_ROLES FOREIGN KEY (role_id) REFERENCES ROLES (role_id)
);

CREATE TABLE TOKENS (
    token_id    VARCHAR2(50) PRIMARY KEY,
    account_id  VARCHAR2(50)  NOT NULL,
    token_value VARCHAR2(500) NOT NULL,
    expiry_date TIMESTAMP     NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_revoked  NUMBER(1) DEFAULT 0,
    is_deleted  NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_TOKENS_ACCOUNTS FOREIGN KEY (account_id) REFERENCES ACCOUNTS (account_id)
);

CREATE TABLE LOGIN_HISTORY (
    log_id         VARCHAR2(50) PRIMARY KEY,
    account_id     VARCHAR2(50) NOT NULL,
    action_type    NVARCHAR2(50),
    ip_address     VARCHAR2(45),
    device_info    NVARCHAR2(255),
    login_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status         NVARCHAR2(20),
    failure_reason NVARCHAR2(255),
    is_deleted     NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_LOG_ACCOUNTS FOREIGN KEY (account_id) REFERENCES ACCOUNTS (account_id)
);
CREATE INDEX IDX_LOGHIS_ACCOUNT_TIME ON LOGIN_HISTORY (account_id, login_time DESC);
CREATE INDEX IDX_LOGHIS_ACTION ON LOGIN_HISTORY (action_type);

CREATE TABLE AUDIT_LOG (
    LOG_ID      VARCHAR2(50) PRIMARY KEY,
    ACCOUNT_ID  VARCHAR2(50),
    ACTION_TYPE VARCHAR2(50) NOT NULL,
    ENTITY_TYPE VARCHAR2(50) NOT NULL,
    ENTITY_ID   VARCHAR2(50) NOT NULL,
    OLD_VALUE   NVARCHAR2(1000),
    NEW_VALUE   NVARCHAR2(1000),
    REASON      NVARCHAR2(255),
    IP_ADDRESS  VARCHAR2(45),
    DEVICE_INFO NVARCHAR2(255),
    CREATED_AT  TIMESTAMP DEFAULT SYSTIMESTAMP,
    IS_DELETED  NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_AUDIT_ACCOUNT FOREIGN KEY (ACCOUNT_ID) REFERENCES ACCOUNTS (ACCOUNT_ID)
);
CREATE INDEX IDX_AUDIT_CREATED ON AUDIT_LOG (CREATED_AT DESC);
CREATE INDEX IDX_AUDIT_ACCOUNT ON AUDIT_LOG (ACCOUNT_ID);
CREATE INDEX IDX_AUDIT_ACTION ON AUDIT_LOG (ACTION_TYPE);


-- ==========================================================
-- 2. HUMAN RESOURCES AND KPI MANAGEMENT
-- ==========================================================

CREATE TABLE SHIFTS (
    shift_id   VARCHAR2(50) PRIMARY KEY,
    shift_name NVARCHAR2(50),
    start_time DATE,
    end_time   DATE,
    is_deleted NUMBER(1) DEFAULT 0
);

CREATE TABLE EMPLOYEES (
    employee_id            VARCHAR2(50) PRIMARY KEY,
    employee_name          NVARCHAR2(100),
    gender                 VARCHAR2(20),
    phone                  VARCHAR2(20),
    email                  VARCHAR2(100),
    hire_date              DATE,
    salary_coefficient     NUMBER(5, 2),
    total_completed_orders NUMBER(10) DEFAULT 0,
    role_id                VARCHAR2(50),
    shift_id               VARCHAR2(50),
    store_id               VARCHAR2(50), -- Gộp trực tiếp Chi nhánh
    is_deleted             NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_EMPLOYEES_ROLES FOREIGN KEY (role_id) REFERENCES ROLES (role_id),
    CONSTRAINT FK_EMPLOYEES_SHIFTS FOREIGN KEY (shift_id) REFERENCES SHIFTS (shift_id),
    CONSTRAINT FK_EMPLOYEES_STORES FOREIGN KEY (store_id) REFERENCES STORES (store_id)
);

CREATE TABLE ACTIVATION_TOKENS (
    TOKEN_ID    NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    EMPLOYEE_ID VARCHAR2(50) NOT NULL,
    CODE        VARCHAR2(100) NOT NULL,
    EXPIRES_AT  DATE NOT NULL,
    USED_AT     DATE,
    CREATED_AT  DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT UQ_ACTIVATION_CODE UNIQUE (CODE),
    CONSTRAINT FK_ACTIVATION_EMP FOREIGN KEY (EMPLOYEE_ID) REFERENCES EMPLOYEES (EMPLOYEE_ID)
);
CREATE INDEX IDX_ACTIVATION_EMP ON ACTIVATION_TOKENS (EMPLOYEE_ID);

CREATE TABLE ATTENDANCE (
    employee_id            VARCHAR2(50),
    shift_id               VARCHAR2(50),
    work_date              DATE,
    check_in_time          TIMESTAMP,
    check_out_time         TIMESTAMP,
    attendance_coefficient NUMBER(3, 1),
    is_deleted             NUMBER(1) DEFAULT 0,
    PRIMARY KEY (employee_id, shift_id),
    CONSTRAINT FK_ATTENDANCE_EMPLOYEES FOREIGN KEY (employee_id) REFERENCES EMPLOYEES (employee_id),
    CONSTRAINT FK_ATTENDANCE_SHIFTS FOREIGN KEY (shift_id) REFERENCES SHIFTS (shift_id)
);

CREATE TABLE KPI_CRITERIA (
    kpi_id         VARCHAR2(50) PRIMARY KEY,
    criteria_name  NVARCHAR2(100),
    criteria_type  NVARCHAR2(50),
    weight         NUMBER(3, 2),
    recorded_time  TIMESTAMP,
    minimum_target NUMBER(15, 2),
    is_deleted     NUMBER(1) DEFAULT 0
);

CREATE TABLE KPI_EVALUATION (
    employee_id       VARCHAR2(50),
    kpi_id            VARCHAR2(50),
    evaluation_period VARCHAR2(20),
    actual_value      NUMBER(15, 2),
    achieved_score    NUMBER(5, 2),
    manager_note      NVARCHAR2(255),
    is_deleted        NUMBER(1) DEFAULT 0,
    PRIMARY KEY (employee_id, kpi_id, evaluation_period),
    CONSTRAINT FK_EVAL_EMPLOYEES FOREIGN KEY (employee_id) REFERENCES EMPLOYEES (employee_id),
    CONSTRAINT FK_EVAL_KPI FOREIGN KEY (kpi_id) REFERENCES KPI_CRITERIA (kpi_id)
);


-- ==========================================================
-- 3. PRODUCTS AND INVENTORY MANAGEMENT
-- ==========================================================

CREATE TABLE CATEGORIES (
    category_id   VARCHAR2(50) PRIMARY KEY,
    category_name NVARCHAR2(100) NOT NULL,
    description   NVARCHAR2(255),
    is_deleted    NUMBER(1) DEFAULT 0
);

CREATE TABLE SUPPLIERS (
    supplier_id   VARCHAR2(50) PRIMARY KEY,
    supplier_name NVARCHAR2(150) NOT NULL,
    email         VARCHAR2(100),
    address       NVARCHAR2(200),
    phone_number  VARCHAR2(20),
    is_deleted    NUMBER(1) DEFAULT 0
);

CREATE TABLE UNITS (
    unit_id     VARCHAR2(50) PRIMARY KEY,
    unit_name   NVARCHAR2(100) NOT NULL,
    description NVARCHAR2(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted  NUMBER(1) DEFAULT 0
);

CREATE TABLE PRODUCTS (
    product_id   VARCHAR2(50) PRIMARY KEY,
    product_name NVARCHAR2(150) NOT NULL,
    base_price   NUMBER(15, 2),
    category_id  VARCHAR2(50),
    supplier_id  VARCHAR2(50),
    base_unit_id VARCHAR2(50),
    image_path   VARCHAR2(255),
    is_deleted   NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_PRODUCTS_CATEGORIES FOREIGN KEY (category_id) REFERENCES CATEGORIES (category_id),
    CONSTRAINT FK_PRODUCTS_SUPPLIERS FOREIGN KEY (supplier_id) REFERENCES SUPPLIERS (supplier_id),
    CONSTRAINT FK_PRODUCTS_BASE_UNIT FOREIGN KEY (base_unit_id) REFERENCES UNITS (unit_id)
);

-- Khởi tạo Sequence và Trigger sinh mã Sản phẩm tự động (SP0000001)
CREATE SEQUENCE product_seq START WITH 1 INCREMENT BY 1;
/
CREATE OR REPLACE TRIGGER trg_product_id
    BEFORE INSERT ON PRODUCTS
    FOR EACH ROW
    WHEN (NEW.product_id IS NULL)
BEGIN
    :NEW.product_id := 'SP' || LPAD(product_seq.NEXTVAL, 7, '0');
END;
/

CREATE TABLE PRODUCT_UNITS (
    product_id              VARCHAR2(50) NOT NULL,
    unit_id                 VARCHAR2(50) NOT NULL,
    conversion_rate_to_base NUMBER(18, 4) DEFAULT 1,
    is_base_unit            NUMBER(1) DEFAULT 0,
    is_deleted              NUMBER(1) DEFAULT 0,
    CONSTRAINT pk_product_units PRIMARY KEY (product_id, unit_id),
    CONSTRAINT FK_PU_PRODUCTS FOREIGN KEY (product_id) REFERENCES PRODUCTS (product_id),
    CONSTRAINT FK_PU_UNITS FOREIGN KEY (unit_id) REFERENCES UNITS (unit_id),
    CONSTRAINT CK_PU_CONVERSION_POSITIVE CHECK (conversion_rate_to_base > 0)
);

CREATE TABLE STORES (
    store_id     VARCHAR2(50) PRIMARY KEY,
    email        VARCHAR2(100),
    address      NVARCHAR2(200),
    phone_number VARCHAR2(20),
    is_deleted   NUMBER(1) DEFAULT 0
);

CREATE TABLE INVENTORY (
    product_id   VARCHAR2(50),
    store_id     VARCHAR2(50),
    quantity     NUMBER DEFAULT 0,
    unit         NVARCHAR2(50),
    last_updated DATE,
    is_deleted   NUMBER(1) DEFAULT 0,
    PRIMARY KEY (product_id, store_id),
    CONSTRAINT FK_INVENTORY_PRODUCTS FOREIGN KEY (product_id) REFERENCES PRODUCTS (product_id),
    CONSTRAINT FK_INVENTORY_STORES FOREIGN KEY (store_id) REFERENCES STORES (store_id)
);


-- ==========================================================
-- 4. SALES AND ORDER FULFILLMENT
-- ==========================================================

CREATE TABLE CUSTOMERS (
    customer_id    VARCHAR2(50) PRIMARY KEY,
    customer_name  NVARCHAR2(100),
    phone          VARCHAR2(20),
    email          VARCHAR2(100),
    address        VARCHAR2(200),
    role_id        VARCHAR2(50),
    total_spending NUMBER(15, 2) DEFAULT 0,
    reward_points  NUMBER(10) DEFAULT 0,
    remember_rank  VARCHAR2(20),
    is_deleted     NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_CUSTOMERS_ROLES FOREIGN KEY (role_id) REFERENCES ROLES (role_id)
);

CREATE TABLE PAYMENT_METHODS (
    payment_method_id VARCHAR2(50) PRIMARY KEY,
    is_deleted        NUMBER(1) DEFAULT 0
);

CREATE TABLE CASH_PAYMENT (
    payment_method_id VARCHAR2(50) PRIMARY KEY,
    is_deleted        NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_CASH_PM FOREIGN KEY (payment_method_id) REFERENCES PAYMENT_METHODS (payment_method_id)
);

CREATE TABLE BANK_TRANSFER_PAYMENT (
    payment_method_id     VARCHAR2(50) PRIMARY KEY,
    bank_name             NVARCHAR2(100),
    transaction_time      TIMESTAMP,
    sender_account_number VARCHAR2(50),
    qr_code               VARCHAR2(255),
    is_deleted            NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_BT_PM FOREIGN KEY (payment_method_id) REFERENCES PAYMENT_METHODS (payment_method_id)
);

CREATE TABLE ORDERS (
    order_id          VARCHAR2(50) PRIMARY KEY,
    customer_id       VARCHAR2(50),
    payment_method_id VARCHAR2(50),
    order_date        DATE,
    status            NVARCHAR2(50),
    total_amount      NUMBER(15, 2),
    note              NVARCHAR2(255),
    employee_id       VARCHAR2(50),
    is_deleted        NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_ORDERS_CUSTOMERS FOREIGN KEY (customer_id) REFERENCES CUSTOMERS (customer_id),
    CONSTRAINT FK_ORDERS_PM FOREIGN KEY (payment_method_id) REFERENCES PAYMENT_METHODS (payment_method_id),
    CONSTRAINT FK_ORDERS_EMPLOYEES FOREIGN KEY (employee_id) REFERENCES EMPLOYEES (employee_id)
);

CREATE TABLE ORDER_DETAILS (
    order_detail_id VARCHAR2(50) PRIMARY KEY,
    order_id        VARCHAR2(50),
    product_id      VARCHAR2(50),
    quantity        NUMBER(10),
    unit_id         VARCHAR2(50),
    quantity_base   NUMBER(10),
    unit_price      NUMBER(15, 2),
    is_deleted      NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_OD_ORDERS FOREIGN KEY (order_id) REFERENCES ORDERS (order_id),
    CONSTRAINT FK_OD_PRODUCTS FOREIGN KEY (product_id) REFERENCES PRODUCTS (product_id),
    CONSTRAINT FK_OD_UNITS FOREIGN KEY (unit_id) REFERENCES UNITS (unit_id)
);

CREATE TABLE DELIVERY_MANAGEMENT (
    delivery_id    VARCHAR2(50) PRIMARY KEY,
    order_id       VARCHAR2(50),
    employee_id    VARCHAR2(50),
    execution_date DATE,
    status         NVARCHAR2(50),
    is_deleted     NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_DM_ORDERS FOREIGN KEY (order_id) REFERENCES ORDERS (order_id),
    CONSTRAINT FK_DM_EMPLOYEES FOREIGN KEY (employee_id) REFERENCES EMPLOYEES (employee_id)
);

CREATE TABLE ON_SITE_PICKUP (
    delivery_id      VARCHAR2(50) PRIMARY KEY,
    counter_position NVARCHAR2(50),
    is_deleted       NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_OSP_DM FOREIGN KEY (delivery_id) REFERENCES DELIVERY_MANAGEMENT (delivery_id)
);

CREATE TABLE STORE_PICKUP (
    delivery_id        VARCHAR2(50) PRIMARY KEY,
    locker_id          VARCHAR2(50),
    pickup_appointment TIMESTAMP,
    is_deleted         NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_SP_DM FOREIGN KEY (delivery_id) REFERENCES DELIVERY_MANAGEMENT (delivery_id)
);

CREATE TABLE HOME_DELIVERY (
    delivery_id      VARCHAR2(50) PRIMARY KEY,
    delivery_address NVARCHAR2(200),
    shipping_fee     NUMBER(15, 2),
    recipient_phone  VARCHAR2(20),
    is_deleted       NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_HD_DM FOREIGN KEY (delivery_id) REFERENCES DELIVERY_MANAGEMENT (delivery_id)
);


-- ==========================================================
-- 5. PROMOTION CAMPAIGNS & APP SETTINGS
-- ==========================================================

CREATE TABLE PROMOTION_CAMPAIGNS (
    campaign_id   VARCHAR2(50) PRIMARY KEY,
    campaign_name NVARCHAR2(150) NOT NULL,
    description   NVARCHAR2(255),
    start_date    DATE,
    end_date      DATE,
    is_deleted    NUMBER(1) DEFAULT 0
);

CREATE TABLE PROMOTIONS (
    promotion_id          VARCHAR2(50) PRIMARY KEY,
    promotion_name        NVARCHAR2(150) NOT NULL,
    campaign_id           VARCHAR2(50),
    application_condition NVARCHAR2(255),
    status                NVARCHAR2(50),
    order_detail_id       VARCHAR2(50),
    discount_amount       NUMBER(15, 2),
    is_deleted            NUMBER(1) DEFAULT 0,
    CONSTRAINT FK_PROMOTIONS_CAMPAIGNS FOREIGN KEY (campaign_id) REFERENCES PROMOTION_CAMPAIGNS (campaign_id),
    CONSTRAINT FK_PROMOTIONS_OD FOREIGN KEY (order_detail_id) REFERENCES ORDER_DETAILS (order_detail_id)
);

CREATE TABLE OTP_STORAGE (
    email       VARCHAR2(150) PRIMARY KEY,
    otp_code    VARCHAR2(6) NOT NULL,
    expiry_time DATE        NOT NULL
);

CREATE TABLE APP_SYNC (
    sync_key       VARCHAR2(50) PRIMARY KEY,
    version_number NUMBER    DEFAULT 0 NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE SYSTEM_CONFIG (
    config_key   VARCHAR2(50) PRIMARY KEY,
    config_value VARCHAR2(500)
);

CREATE TABLE INVENTORY_NOTIFICATIONS (
    notification_id VARCHAR2(50) PRIMARY KEY,
    product_id      VARCHAR2(50) NOT NULL,
    product_name    NVARCHAR2(255),
    message         NVARCHAR2(1000),
    target_role     VARCHAR2(50) DEFAULT 'WAREHOUSE',
    status          VARCHAR2(20) DEFAULT 'PENDING',
    remind_count    NUMBER DEFAULT 1,
    created_by      VARCHAR2(50),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at     TIMESTAMP,
    is_deleted      NUMBER(1) DEFAULT 0
);

-- ==========================================================
-- PURCHASE RECEIPTS / INVENTORY TRANSACTION HISTORY
-- Quản lý phiếu nhập hàng và lịch sử biến động kho
-- ==========================================================

CREATE TABLE PURCHASE_RECEIPTS (
    receipt_id         VARCHAR2(50) PRIMARY KEY,
    supplier_id        VARCHAR2(50),
    created_by         VARCHAR2(50),
    note               NVARCHAR2(1000),

    total_before_tax   NUMBER(18,2) DEFAULT 0,
    total_tax          NUMBER(18,2) DEFAULT 0,
    total_after_tax    NUMBER(18,2) DEFAULT 0,

    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted         NUMBER(1) DEFAULT 0
);

CREATE TABLE PURCHASE_RECEIPT_DETAILS (
    receipt_detail_id  VARCHAR2(50) PRIMARY KEY,
    receipt_id         VARCHAR2(50) NOT NULL,
    product_id         VARCHAR2(50) NOT NULL,

    quantity           NUMBER(10) NOT NULL,
    unit               NVARCHAR2(50),

    unit_import_price  NUMBER(18,2) NOT NULL,
    sale_price         NUMBER(18,2) NOT NULL,
    vat_rate           NUMBER(5,2) DEFAULT 0,

    line_before_tax    NUMBER(18,2) DEFAULT 0,
    line_tax           NUMBER(18,2) DEFAULT 0,
    line_after_tax     NUMBER(18,2) DEFAULT 0,

    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted         NUMBER(1) DEFAULT 0,

    CONSTRAINT FK_PRD_RECEIPT
        FOREIGN KEY (receipt_id)
        REFERENCES PURCHASE_RECEIPTS(receipt_id),

    CONSTRAINT FK_PRD_PRODUCT
        FOREIGN KEY (product_id)
        REFERENCES PRODUCTS(product_id)
);

CREATE TABLE INVENTORY_TRANSACTIONS (
    transaction_id     VARCHAR2(50) PRIMARY KEY,
    receipt_id         VARCHAR2(50),
    product_id         VARCHAR2(50) NOT NULL,

    transaction_type   VARCHAR2(30) NOT NULL,
    quantity           NUMBER(10) NOT NULL,
    unit               NVARCHAR2(50),
    store_id           VARCHAR2(50),

    unit_import_price  NUMBER(18,2),
    sale_price         NUMBER(18,2),
    vat_rate           NUMBER(5,2),
    vat_amount         NUMBER(18,2),
    total_amount       NUMBER(18,2),

    note               NVARCHAR2(1000),
    created_by         VARCHAR2(50),

    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted         NUMBER(1) DEFAULT 0,

    CONSTRAINT FK_INV_TRANS_RECEIPT
        FOREIGN KEY (receipt_id)
        REFERENCES PURCHASE_RECEIPTS(receipt_id),

    CONSTRAINT FK_INV_TRANS_PRODUCT
        FOREIGN KEY (product_id)
        REFERENCES PRODUCTS(product_id),

    CONSTRAINT CK_INV_TRANS_TYPE
        CHECK (transaction_type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT', 'CANCEL'))
);

CREATE INDEX IDX_PURCHASE_RECEIPTS_CREATED_AT
ON PURCHASE_RECEIPTS(created_at);

CREATE INDEX IDX_PURCHASE_DETAILS_RECEIPT
ON PURCHASE_RECEIPT_DETAILS(receipt_id);

CREATE INDEX IDX_PURCHASE_DETAILS_PRODUCT
ON PURCHASE_RECEIPT_DETAILS(product_id);

CREATE INDEX IDX_INV_TRANS_PRODUCT_TIME
ON INVENTORY_TRANSACTIONS(product_id, created_at);

CREATE INDEX IDX_INV_TRANS_RECEIPT
ON INVENTORY_TRANSACTIONS(receipt_id);
