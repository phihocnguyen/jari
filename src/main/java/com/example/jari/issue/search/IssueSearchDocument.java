package com.example.jari.issue.search;

import lombok.Data;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Đọc từ index "jari-issues" do Logstash indexer đồng bộ từ PostgreSQL
 * (monitoring/logstash/pipeline/indexer.conf). Index được tạo bởi index
 * template (monitoring/elasticsearch/templates/jari-issues.json) nên app
 * không tự tạo mapping.
 *
 * Chỉ cần các field dùng để filter/sort; response đầy đủ vẫn hydrate từ DB.
 */
@Data
@Document(indexName = "jari-issues", createIndex = false)
public class IssueSearchDocument {

    @Id
    private String id;

    @Field(name = "issue_key", type = FieldType.Keyword)
    private String issueKey;

    @Field(name = "issue_key_lower", type = FieldType.Keyword)
    private String issueKeyLower;

    @Field(name = "title_lower", type = FieldType.Keyword)
    private String titleLower;

    @Field(name = "project_id", type = FieldType.Keyword)
    private String projectId;

    @Field(name = "status_id", type = FieldType.Keyword)
    private String statusId;

    @Field(name = "assignee_id", type = FieldType.Keyword)
    private String assigneeId;

    @Field(name = "issue_type_id", type = FieldType.Keyword)
    private String issueTypeId;

    @Field(name = "priority_id", type = FieldType.Keyword)
    private String priorityId;

    @Field(name = "sprint_ids", type = FieldType.Keyword)
    private List<String> sprintIds;

    @Field(name = "position", type = FieldType.Double)
    private Double position;

    @Field(name = "created_at", type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime createdAt;
}
