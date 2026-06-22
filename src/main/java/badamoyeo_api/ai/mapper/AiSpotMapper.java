package badamoyeo_api.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import badamoyeo_api.ai.dto.AiSpotSearchCondition;
import badamoyeo_api.ai.dto.AiSpotSearchResult;

@Mapper
public interface AiSpotMapper {
	List<AiSpotSearchResult> searchSpots(AiSpotSearchCondition condition);
}
