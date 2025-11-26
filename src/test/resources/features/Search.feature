Feature: Search
  Background:
    Given The user is on the homepage

  Scenario: User Search query is Accepted
    When they type a search query into the search bar
    Then the search query should be accepted and processed

  Scenario: Only Relative Items Returned in the results
    Given The user has entered a search query
    When they click the Search button or press Enter key
    Then the search results page should display items matching the search query

  Scenario: Verify Search result updated according to the applied filter
      Given The search results are displayed
      When the user applies filters (eg brand)
      Then the search results should be filtered accordingly

  Scenario: Verify Search result updated according to the applied Sorting manner
      Given The search results are displayed
      When the user chooses to sort the results (eg price)
      Then the search results should be sorted accordingly
  @run
  Scenario: Verify that user can search with product Company name
      Given the user enters Company name in the search field
      When they click the Search button or press Enter key
      Then company products should be displayed
  @run
  Scenario: Verify that user can search with partially product name
      Given the user enters product name partially in the search field
      When they click the Search button or press Enter key
      Then products relevant to that name should be displayed
