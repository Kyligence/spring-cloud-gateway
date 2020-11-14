package io.kyligence.kap.gateway.route.reader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.persistent.FileDataSource;
import io.kyligence.kap.gateway.persistent.domain.RouteDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;

@Slf4j
public class FileRouteTableReader implements IRouteTableReader {

	//TODO Save inMemory route table to file .
	private String routeTableFile;

	private ObjectMapper routeDOMapper = new ObjectMapper();

	public FileRouteTableReader(FileDataSource fileDataSource) {
		this.routeTableFile = fileDataSource.getRouteTableFilePath();
	}

	@Override
	public List<KylinRouteRaw> list() {
		List<KylinRouteRaw> result = Lists.newArrayList();
		try {
			List<String> routeTable = FileUtils.readLines(new File(routeTableFile));
			int i = 0;
			for (String routeStr : routeTable) {
				if (StringUtils.isNotBlank(routeStr)) {
					RouteDO route = routeDOMapper.readValue(routeStr, RouteDO.class);
					route.setId(i++);
					result.add(KylinRouteRaw.convert(route));
				}
			}
		} catch (JsonProcessingException e) {
			log.error("Route format example: {\"order\":10, \"clusterId\":\"6f6c0dd7-43cf-437a-9527-176ed2d2ab65\", \"backends\":[\"10.1.2.56:7070\"], \"project\":\"p2\", \"type\":\"QUERY\"}", e);
		} catch (Exception e) {
			log.error("Failed to read route table from file: {}", routeTableFile, e);
		}

		return result;
	}
}
