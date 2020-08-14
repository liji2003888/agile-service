package script.db.groovy.agile_service

databaseChangeLog(logicalFilePath: 'script/db/fd_status_notice_setting.groovy') {
    changeSet(author: "jiaxu.cui@hand-china.com", id: "2020-08-12-fd_status_notice_setting"){
        createTable(tableName: "fd_status_notice_setting", remarks: "状态变更消息通知") {
            column(name: "id", type: "BIGINT UNSIGNED", autoIncrement: true, remarks: "id") {
                constraints(primaryKey: true)
            }
            column(name: "issue_type_id", type: "BIGINT UNSIGNED",  remarks: "issue_type_id") {
                constraints(nullable:"false")
            }
            column(name: "project_id", type: "BIGINT UNSIGNED",  remarks: "project_id") {
                constraints(nullable:"false")
            }
            column(name: "status_id", type: "BIGINT UNSIGNED",  remarks: "status_id") {
                constraints(nullable:"false")
            }
            column(name: "user_type", type: "varchar(255)",  remarks: "projectOwner, assignee, reporter, specifier") {
                constraints(nullable:"false")
            }
            column(name: "user_id", type: "BIGINT UNSIGNED",  remarks: "user_id")
            column(name: "notice_type", type: "varchar(255)",  remarks: "notice_type") {
                constraints(nullable:"false")
            }

            column(name: "object_version_number", type: "BIGINT UNSIGNED", defaultValue: "1")
            column(name: "created_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "creation_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
            column(name: "last_updated_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "last_update_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
        }
    }
}