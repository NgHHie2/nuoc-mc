-- Tạo các database cho từng service
CREATE DATABASE accountservicedb;
CREATE DATABASE statsservicedb;
CREATE DATABASE learnservicedb;

-- Cấp quyền cho user postgres
GRANT ALL PRIVILEGES ON DATABASE accountservicedb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE statsservicedb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE learnservicedb TO postgres;