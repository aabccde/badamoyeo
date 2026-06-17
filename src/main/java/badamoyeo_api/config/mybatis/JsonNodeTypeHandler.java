package badamoyeo_api.config.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@MappedTypes(JsonNode.class)
public class JsonNodeTypeHandler extends BaseTypeHandler<JsonNode> {
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, JsonNode parameter, JdbcType jdbcType)
		throws SQLException {
		ps.setString(i, parameter.toString());
	}

	@Override
	public JsonNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return parse(rs.getString(columnName));
	}

	@Override
	public JsonNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return parse(rs.getString(columnIndex));
	}

	@Override
	public JsonNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return parse(cs.getString(columnIndex));
	}

	private JsonNode parse(String value) throws SQLException {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return objectMapper.readTree(value);
		} catch (Exception exception) {
			throw new SQLException("Failed to parse JSON column", exception);
		}
	}
}
