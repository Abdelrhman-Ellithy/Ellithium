package UI_BDD;
import Ellithium.Utilities.assertion.AssertionExecutor;
import Ellithium.core.driver.*;
import Pages.NoonSearchPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SearchStepDefinitions {
    public NoonSearchPage searchPage;
    @Given("The user is on the homepage")
    public void the_user_is_on_the_homepage()  {
            searchPage=new NoonSearchPage(DriverFactory.getCurrentDriver());
            searchPage.returnHome();
    }
    @When("they type a search query into the search bar")
    public void they_type_a_search_query_into_the_search_bar() {
        searchPage.searchItem("laptop");
    }

    @Then("the search query should be accepted and processed")
    public void the_search_query_should_be_accepted_and_processed() {
        String actualText=searchPage.getTextInSearchField();
        String expectedText="laptop";
        AssertionExecutor.hard.assertTrue(actualText.toLowerCase().contains(expectedText));
    }

    @Given("The user has entered a search query")
    public void the_user_has_entered_a_search_query() {
        searchPage.searchItem("dell");
    }
    @When("they click the Search button or press Enter key")
    public void they_click_the_button_or_press_enter_key() {
        searchPage.clickEnter();
    }

    @Then("the search results page should display items matching the search query")
    public void the_search_results_page_should_display_items_matching_the_search_query() {
        List<String>names=searchPage.getResultsNames();
        byte cnt=0;
        for (String name:names){
            cnt++;
            AssertionExecutor.hard.assertTrue(name.toLowerCase().contains("dell"));
            if(cnt>4) break;
        }
    }

    @Given("The search results are displayed")
    public void the_search_results_are_displayed() {
        searchPage.searchItem("dell");
        searchPage.clickEnter();
    }

    @When("the user applies filters \\(eg brand)")
    public void the_user_applies_filters_e_g_category_price_brand() {
        searchPage.clickDell();
    }

    @Then("the search results should be filtered accordingly")
    public void the_search_results_should_be_filtered_accordingly() {
        List<String>names=searchPage.getResultsNames();
        for (String name:names){
            AssertionExecutor.hard.assertTrue(name.toLowerCase().contains("dell"));
        }
    }

    @When("the user chooses to sort the results \\(eg price)")
    public void the_user_chooses_to_sort_the_results_e_g_by_relevance_price_popularity()  {
            searchPage.clickSortBy("price low to high");
    }

    @Then("the search results should be sorted accordingly")
    public void the_search_results_should_be_sorted_accordingly() {
        List<String> prices = searchPage.getResultsPrice();

        List<Double> actualPrices = prices.stream()
                .map(p -> Double.parseDouble(p.replaceAll("[^0-9.]", "")))
                .collect(Collectors.toList());

        int limit = Math.min(10, actualPrices.size());
        List<Double> actualFirst10 = actualPrices.subList(0, limit);

        List<Double> expectedFirst10 = new ArrayList<>(actualFirst10);
        expectedFirst10.sort(Comparator.naturalOrder());

        System.out.println("Expected (ascending, first " + limit + "): " + expectedFirst10);
        System.out.println("Actual (first " + limit + "): " + actualFirst10);
        AssertionExecutor.hard.assertEquals(expectedFirst10, actualFirst10);
    }


    @Given("The user enters a search query")
    public void the_user_enters_a_search_query() {
        searchPage.searchItem("laptop");
    }

    @Given("the user enters Company name in the search field")
    public void the_user_enters_company_name_in_the_search_field() {
        searchPage.searchItem("dell");
    }

    @Then("company products should be displayed")
    public void company_products_should_be_displayed() {
        List<String>names=searchPage.getResultsNames();
        var softAssert=new AssertionExecutor.soft();
        byte counter=0;
        for (String name:names){
            counter++;
            if (counter>5)break;
           softAssert.assertTrue(name.toLowerCase().contains("dell"));
        }
        softAssert.assertAll();
    }

    @Given("the user enters product name partially in the search field")
    public void the_user_enters_product_name_partially_in_the_search_field() {
            searchPage.searchItem("lapt");
            searchPage.clickEnter();
    }

    @Then("products relevant to that name should be displayed")
    public void products_relevant_to_that_name_should_be_displayed() {
        List<String>names=searchPage.getResultsNames();
        var softAssert=new AssertionExecutor.soft();
        byte cnt=0;
        for (String name:names ){
            if(cnt>5) break;
            softAssert.assertContains(name.toLowerCase(),"laptop");
            cnt++;
        }
        softAssert.assertAll();
    }
}
