package badamoyeo_api.config.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@MappedTypes(Map.class)
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, Object>> {
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType)
		throws SQLException {
		try {
			ps.setString(i, objectMapper.writeValueAsString(parameter));
		} catch (Exception exception) {
			throw new SQLException("Failed to serialize JSON column", exception);
		}
	}

	@Override
	public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return parse(rs.getString(columnName));
	}

	@Override
	public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return parse(rs.getString(columnIndex));
	}

	@Override
	public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return parse(cs.getString(columnIndex));
	}

	private Map<String, Object> parse(String value) throws SQLException {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return objectMapper.readValue(value, MAP_TYPE);
		} catch (Exception exception) {
			throw new SQLException("Failed to parse JSON column", exception);
		}
	}
}
