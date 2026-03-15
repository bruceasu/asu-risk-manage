package me.asu.ta.ai.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import me.asu.ta.ai.model.PromptTemplate;
import me.asu.ta.ai.model.PromptTemplateType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PromptTemplateRepository {
    private static final RowMapper<PromptTemplate> ROW_MAPPER = new PromptTemplateRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PromptTemplateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(PromptTemplate template) {
        return jdbcTemplate.update("""
                insert into ai_prompt_template(
                    template_code, version, template_type, template_content, is_active,
                    created_at, updated_at, created_by, change_note
                ) values (
                    :templateCode, :version, :templateType, :templateContent, :active,
                    :createdAt, :updatedAt, :createdBy, :changeNote
                )
                """, new MapSqlParameterSource()
                .addValue("templateCode", template.templateCode())
                .addValue("version", template.version())
                .addValue("templateType", template.templateType().name())
                .addValue("templateContent", template.templateContent())
                .addValue("active", template.active())
                .addValue("createdAt", Timestamp.from(template.createdAt()))
                .addValue("updatedAt", Timestamp.from(template.updatedAt()))
                .addValue("createdBy", template.createdBy())
                .addValue("changeNote", template.changeNote()));
    }

    public Optional<PromptTemplate> findActiveTemplate(String templateCode, PromptTemplateType templateType) {
        List<PromptTemplate> rows = jdbcTemplate.query("""
                select *
                  from ai_prompt_template
                 where template_code = :templateCode
                   and template_type = :templateType
                   and is_active = true
                 order by version desc
                 limit 1
                """, new MapSqlParameterSource()
                .addValue("templateCode", templateCode)
                .addValue("templateType", templateType.name()), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<PromptTemplate> findByTemplateCode(String templateCode) {
        return jdbcTemplate.query("""
                select *
                  from ai_prompt_template
                 where template_code = :templateCode
                 order by version desc
                """, new MapSqlParameterSource("templateCode", templateCode), ROW_MAPPER);
    }

    private static final class PromptTemplateRowMapper implements RowMapper<PromptTemplate> {
        @Override
        public PromptTemplate mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PromptTemplate(
                    rs.getString("template_code"),
                    rs.getInt("version"),
                    PromptTemplateType.valueOf(rs.getString("template_type")),
                    rs.getString("template_content"),
                    rs.getBoolean("is_active"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant(),
                    rs.getString("created_by"),
                    rs.getString("change_note"));
        }
    }
}
