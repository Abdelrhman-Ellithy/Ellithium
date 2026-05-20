Scenario: User logs in with valid credentials
  Given the user is on the login page
  When the user enters username 'tomsmith' and password 'SuperSecretPassword!'
  And the user clicks the login button
  Then the user should be logged in successfully
