package Ellithium.Utilities.generators;

import org.testng.annotations.Test;

import static Ellithium.Utilities.assertion.AssertionExecutor.hard.*;

public class TestDataGeneratorTests {

    private void assertNonBlank(String value, String label) {
        assertNotNull(value, label + " must not be null");
        assertFalse(value.isBlank(), label + " must not be blank");
    }

    @Test public void testGetRandomFullName()          { assertNonBlank(TestDataGenerator.getRandomFullName(), "fullName"); }
    @Test public void testGetTimeStamp()               { assertNonBlank(TestDataGenerator.getTimeStamp(), "timestamp"); }
    @Test public void testGetDayDateStamp()            { assertNonBlank(TestDataGenerator.getDayDateStamp(), "dayDateStamp"); }
    @Test public void testGetRandomFirstName()         { assertNonBlank(TestDataGenerator.getRandomFirstName(), "firstName"); }
    @Test public void testGetRandomLastName()          { assertNonBlank(TestDataGenerator.getRandomLastName(), "lastName"); }
    @Test public void testGetRandomEmail()             { assertNonBlank(TestDataGenerator.getRandomEmail(), "email"); }
    @Test public void testGetRandomPhoneNumber()       { assertNonBlank(TestDataGenerator.getRandomPhoneNumber(), "phoneNumber"); }
    @Test public void testGetRandomAddress()           { assertNonBlank(TestDataGenerator.getRandomAddress(), "address"); }
    @Test public void testGetRandomCity()              { assertNonBlank(TestDataGenerator.getRandomCity(), "city"); }
    @Test public void testGetRandomCountry()           { assertNonBlank(TestDataGenerator.getRandomCountry(), "country"); }
    @Test public void testGetRandomState()             { assertNonBlank(TestDataGenerator.getRandomState(), "state"); }
    @Test public void testGetRandomZipCode()           { assertNonBlank(TestDataGenerator.getRandomZipCode(), "zipCode"); }
    @Test public void testGetRandomUsername()          { assertNonBlank(TestDataGenerator.getRandomUsername(), "username"); }
    @Test public void testGetRandomPassword()          { assertNonBlank(TestDataGenerator.getRandomPassword(), "password"); }
    @Test public void testGetRandomCompany()           { assertNonBlank(TestDataGenerator.getRandomCompany(), "company"); }
    @Test public void testGetRandomJobTitle()          { assertNonBlank(TestDataGenerator.getRandomJobTitle(), "jobTitle"); }
    @Test public void testGetRandomWebsite()           { assertNonBlank(TestDataGenerator.getRandomWebsite(), "website"); }
    @Test public void testGetRandomIPAddress()         { assertNonBlank(TestDataGenerator.getRandomIPAddress(), "ipAddress"); }
    @Test public void testGetRandomBirthDate()         { assertNonBlank(TestDataGenerator.getRandomBirthDate(), "birthDate"); }
    @Test public void testGetRandomCreditCardNumber()  { assertNonBlank(TestDataGenerator.getRandomCreditCardNumber(), "creditCard"); }
    @Test public void testGetRandomCreditCardExpiry()  { assertNonBlank(TestDataGenerator.getRandomCreditCardExpiry(), "creditCardExpiry"); }
    @Test public void testGetMedicineName()            { assertNonBlank(TestDataGenerator.getMedicineName(), "medicineName"); }
    @Test public void testGetRandomUniversity()        { assertNonBlank(TestDataGenerator.getRandomUniversity(), "university"); }
    @Test public void testGetRandomDegree()            { assertNonBlank(TestDataGenerator.getRandomDegree(), "degree"); }
    @Test public void testGetRandomAnimal()            { assertNonBlank(TestDataGenerator.getRandomAnimal(), "animal"); }
    @Test public void testGetRandomColor()             { assertNonBlank(TestDataGenerator.getRandomColor(), "color"); }
    @Test public void testGetRandomBook()              { assertNonBlank(TestDataGenerator.getRandomBook(), "book"); }
    @Test public void testGetRandomSentence()          { assertNonBlank(TestDataGenerator.getRandomSentence(), "sentence"); }
    @Test public void testGetRandomParagraph()         { assertNonBlank(TestDataGenerator.getRandomParagraph(), "paragraph"); }
    @Test public void testGetRandomQuote()             { assertNonBlank(TestDataGenerator.getRandomQuote(), "quote"); }
    @Test public void testGetRandomCountryCode()       { assertNonBlank(TestDataGenerator.getRandomCountryCode(), "countryCode"); }
    @Test public void testGetRandomBuildingNumber()    { assertNonBlank(TestDataGenerator.getRandomBuildingNumber(), "buildingNumber"); }
    @Test public void testGetRandomStreetName()        { assertNonBlank(TestDataGenerator.getRandomStreetName(), "streetName"); }
    @Test public void testGetRandomStreetAddress()     { assertNonBlank(TestDataGenerator.getRandomStreetAddress(), "streetAddress"); }
    @Test public void testGetRandomLatitude()          { assertNonBlank(TestDataGenerator.getRandomLatitude(), "latitude"); }
    @Test public void testGetRandomLongitude()         { assertNonBlank(TestDataGenerator.getRandomLongitude(), "longitude"); }
    @Test public void testGetRandomTimeZone()          { assertNonBlank(TestDataGenerator.getRandomTimeZone(), "timeZone"); }
    @Test public void testGetRandomBankAccountNumber() { assertNonBlank(TestDataGenerator.getRandomBankAccountNumber(), "bankAccount"); }
    @Test public void testGetRandomSWIFTCode()         { assertNonBlank(TestDataGenerator.getRandomSWIFTCode(), "swiftCode"); }
    @Test public void testGetRandomCompanyIndustry()   { assertNonBlank(TestDataGenerator.getRandomCompanyIndustry(), "industry"); }
    @Test public void testGetRandomCompanyCatchPhrase(){ assertNonBlank(TestDataGenerator.getRandomCompanyCatchPhrase(), "catchPhrase"); }
    @Test public void testGetRandomProductName()       { assertNonBlank(TestDataGenerator.getRandomProductName(), "productName"); }
    @Test public void testGetRandomProductPrice()      { assertNonBlank(TestDataGenerator.getRandomProductPrice(), "productPrice"); }
    @Test public void testGetRandomProductMaterial()   { assertNonBlank(TestDataGenerator.getRandomProductMaterial(), "productMaterial"); }
    @Test public void testGetRandomDepartment()        { assertNonBlank(TestDataGenerator.getRandomDepartment(), "department"); }
    @Test public void testGetRandomCurrencyCode()      { assertNonBlank(TestDataGenerator.getRandomCurrencyCode(), "currencyCode"); }
    @Test public void testGetRandomCurrencyName()      { assertNonBlank(TestDataGenerator.getRandomCurrencyName(), "currencyName"); }
    @Test public void testGetRandomFunnyName()         { assertNonBlank(TestDataGenerator.getRandomFunnyName(), "funnyName"); }

    @Test
    public void testTimestampFormat() {
        String ts = TestDataGenerator.getTimeStamp();
        assertTrue(ts.matches("\\d{4}-\\d{2}-\\d{2}-.+"), "Timestamp must match yyyy-MM-dd-... pattern, got: " + ts);
    }

    @Test
    public void testDayDateStampFormat() {
        String ds = TestDataGenerator.getDayDateStamp();
        assertTrue(ds.matches("\\d{4}-\\d{2}-\\d{2}"), "Date stamp must match yyyy-MM-dd pattern, got: " + ds);
    }

    @Test
    public void testEmailContainsAtSign() {
        String email = TestDataGenerator.getRandomEmail();
        assertTrue(email.contains("@"), "Email must contain '@', got: " + email);
    }

    @Test
    public void testIPAddressFormat() {
        String ip = TestDataGenerator.getRandomIPAddress();
        assertTrue(ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"), "IP must be dotted-quad, got: " + ip);
    }

    @Test
    public void testTwoCallsReturnDifferentFullNames() {
        String n1 = TestDataGenerator.getRandomFullName();
        String n2 = TestDataGenerator.getRandomFullName();
        assertFalse(n1.equals(n2), "Two random full names should differ (probabilistic — fails ~1/N² of the time)");
    }
}
