package models;

import org.junit.Test;

import models.searchResult.SearchResultItem;
import models.searchResult.SearchResults;
import static org.junit.Assert.assertEquals;

import java.util.List;

public class SearchResultTest {
    
    // Owner owner = helper.getDummyOwner();
    // SearchResultItem searchResultItem = helper.getDummySearchResultItem();
    SearchResults searchResults = new SearchResults();


    @Test
    public void getItems() {
        List<SearchResultItem> searchResultItems = helper.getDummySearchResults();
        searchResults.setItems(searchResultItems);
        assertEquals(searchResults.getItems(), searchResultItems);
    }

}
