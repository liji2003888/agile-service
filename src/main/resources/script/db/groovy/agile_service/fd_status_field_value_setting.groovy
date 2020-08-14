package script.db.groovy.agile_service

databaseChangeLog(logicalFilePath: 'fd_status_field_value_setting.groovy') {
    changeSet(author: 'ztxemail@163.com', id: '2020-08-13-create-table-status-field-value-setting') {
        createTable(tableName: 'fd_status_field_value_setting') {
            column(name: 'id', type: 'BIGINT UNSIGNED', autoIncrement: 'true', remarks: 'ID,主键') {
                constraints(primaryKey: true)
            }
            column(name: 'status_field_setting_id', type: 'BIGINT UNSIGNED', remarks: '状态字段设置Id') {
                constraints(nullable: false)
            }
            column(name: 'project_id', type: 'BIGINT UNSIGNED', remarks: '项目id') {
                constraints(nullable: false)
            }
            column(name: 'field_type', type: 'VARCHAR(255)', remarks: 'field type')
            column(name: 'operate_type', type: 'VARCHAR(255)', remarks: 'operate type')
            column(name: 'option_id', type: 'BIGINT UNSIGNED', remarks: 'option id')
            column(name: 'string_value', type: 'VARCHAR(255)', remarks: 'string value')
            column(name: 'number_value', type: 'DECIMAL(10,2)', remarks: 'number value')
            column(name: 'number_add_value', type: 'DECIMAL(10,2)', remarks: 'number add value')
            column(name: 'text_value', type: 'TEXT', remarks: 'text value')
            column(name: 'date_value', type: 'DATETIME', remarks: 'date value')
            column(name: 'date_add_value', type: 'DECIMAL(10,0)', remarks: 'date add value')
            column(name: 'user_id', type: 'BIGINT UNSIGNED', remarks: 'user id')

            column(name: "object_version_number", type: "BIGINT UNSIGNED", defaultValue: "1")
            column(name: "created_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "creation_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
            column(name: "last_updated_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "last_update_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
        }
    }
}