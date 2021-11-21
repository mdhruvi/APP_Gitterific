package models.searchResult;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "name",
        "full_name",
        "node_id",
        "owner",
        "topics",
        "snippet"
})

public class SearchResultItem {

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("node_id")
    private String nodeId;

    @JsonProperty("owner")
    private Owner owner;

    @JsonProperty("topics")
    private List<String> topics = null;
    
    @JsonProperty("snippet")
    private Snippet snippet;

    public SearchResultItem() {
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Owner getOwner() {
        return this.owner;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public List<String> getTopics() {
        return this.topics;
    }
    public Snippet getSnippet() {
        return snippet;
    }

    /**
     * Sets snippet.
     *
     * @param snippet the snippet
     */
    public void setSnippet(Snippet snippet) {
        this.snippet = snippet;
    }

    @Override
    public String toString() {
        return "SearchResultItem{" +
                "id=" + Integer.toString(id) +
                ",Snippet=" + snippet +
                ", full_name=" + fullName +
                ", node_id=" + nodeId +
                ", owner=" + owner + "}";
    }
}
