package io.kyligence.kap.gateway.persistent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteDO {

	private long id;

	private int order = 0;

	private String cluster;

	private List<String> backends;

	private String project;

	private String type;

	private String resourceGroup;
}
