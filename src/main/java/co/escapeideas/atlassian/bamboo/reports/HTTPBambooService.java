/**
 * 
 */
package co.escapeideas.atlassian.bamboo.reports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author tmullender
 *
 */
public class HTTPBambooService implements BambooService {
	
	private final RestClient httpClient;
	private final JsonParser parser;

	/**
	 * 
	 */
	public HTTPBambooService(RestClient client, JsonParser parser) {
		this.httpClient = client;
		this.parser = parser;
	}

	/* (non-Javadoc)
	 * @see co.escapeideas.atlassian.bamboo.BambooService#getArtifacts(java.lang.String)
	 */
	@Override
	public List<Build> getArtifacts(String url) {
		final Resource resource = httpClient.resource(url);
		final ClientResponse response = resource.get();
		final List<Build> builds;
		if (isSuccessful(response)){
			final JsonElement element = parser.parse(response.getEntity(String.class));
			builds = toList(element);
		} else {
			builds = getErrorList(response, url);
		}
		return builds;
	}

	private List<Build> getErrorList(ClientResponse response, String url) {
		final ArrayList<Build> list = new ArrayList<Build>();
		final Map<String, String> artifacts = new HashMap<String, String>();
		artifacts.put("ERROR", url);
		list.add(new Build(response.getMessage(), response.getStatusCode(), artifacts ));
		return list;
	}

	private boolean isSuccessful(ClientResponse response) {
		return response != null && HttpStatus.SC_OK == response.getStatusCode();
	}

	/**
	 * Converts the response into a list of Builds
	 * @param response
	 * @return
	 */
	private List<Build> toList(JsonElement response) {
		final ArrayList<Build> list = new ArrayList<Build>();
		final JsonElement results = response.getAsJsonObject().get("results");
		final JsonElement resultArray = results.getAsJsonObject().get("result");
		for (JsonElement result : resultArray.getAsJsonArray()) {
			final Build build = toBuild(result);
			list.add(build);
		}
		return list;
	}

	/**
	 * Converts the result into a Build
	 * @param result
	 * @return
	 */
	private Build toBuild(JsonElement result) {
		final JsonObject resultObject = result.getAsJsonObject();
		final int id = resultObject.get("buildNumber").getAsInt();
		final String status = resultObject.get("buildState").getAsString();
		final Map<String, String> artifactMap = toArtifactMap(resultObject);
		final Build build = new Build(status, id, artifactMap);
		return build;
	}

	/**
	 * Converts the resultObject into a Map of artifact name to url
	 * @param resultObject
	 * @return
	 */
	private Map<String, String> toArtifactMap(final JsonObject resultObject) {
		final Map<String, String> artifactMap = new HashMap<String, String>();
		final JsonElement artifacts = resultObject.get("artifacts");
		final JsonElement artifactArray = artifacts.getAsJsonObject().get("artifact");
		for (JsonElement artifact : artifactArray.getAsJsonArray()){
			final JsonObject object = artifact.getAsJsonObject();
			final String name = object.get("name").getAsString();
			final String url = object.get("link").getAsJsonObject().get("href").getAsString();
			artifactMap.put(name, url);
		}
		return artifactMap;
	}

}
