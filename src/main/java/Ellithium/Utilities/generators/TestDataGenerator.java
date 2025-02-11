package Ellithium.Utilities.generators;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.github.javafaker.Faker;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Generates random test data using JavaFaker.
 */
public class TestDataGenerator {
    private static final Faker faker = new Faker();

    /**
     * Generates a random full name.
     * @return A random full name
     */
    public static String getRandomFullName() {
        Reporter.log("Generating random FullName", LogLevel.INFO_BLUE);
        return faker.name().fullName();
    }

    /**
     * Gets current timestamp in yyyy-MM-dd-h-m-ssa format.
     * @return Formatted timestamp
     */
    public static String getTimeStamp() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-h-m-ssa").format(new Date());
        Reporter.log("Getting Timestamp: " + timestamp, LogLevel.INFO_BLUE);
        return timestamp;
    }

    /**
     * Gets current date in yyyy-MM-dd format.
     * @return Formatted date
     */
    public static String getDayDateStamp() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Reporter.log("Getting Date: " + timestamp, LogLevel.INFO_BLUE);
        return timestamp;
    }

    /**
     * Generates a random first name.
     * @return A random first name
     */
    public static String getRandomFirstName() {
        Reporter.log("Generating random FirstName", LogLevel.INFO_BLUE);
        return faker.name().firstName();
    }

    /**
     * Generates a random last name.
     * @return A random last name
     */
    public static String getRandomLastName() {
        Reporter.log("Generating random LastName", LogLevel.INFO_BLUE);
        return faker.name().lastName();
    }

    /**
     * Generates a random email.
     * @return A random email
     */
    public static String getRandomEmail() {
        Reporter.log("Generating random Email", LogLevel.INFO_BLUE);
        return faker.internet().emailAddress();
    }

    /**
     * Generates a random phone number.
     * @return A random phone number
     */
    public static String getRandomPhoneNumber() {
        Reporter.log("Generating random PhoneNumber", LogLevel.INFO_BLUE);
        return faker.phoneNumber().cellPhone();
    }

    /**
     * Generates a random address.
     * @return A random address
     */
    public static String getRandomAddress() {
        Reporter.log("Generating random Address", LogLevel.INFO_BLUE);
        return faker.address().fullAddress();
    }

    /**
     * Generates a random city.
     * @return A random city
     */
    public static String getRandomCity() {
        Reporter.log("Generating random City", LogLevel.INFO_BLUE);
        return faker.address().city();
    }

    /**
     * Generates a random country.
     * @return A random country
     */
    public static String getRandomCountry() {
        Reporter.log("Generating random Country", LogLevel.INFO_BLUE);
        return faker.address().country();
    }

    /**
     * Generates a random state.
     * @return A random state
     */
    public static String getRandomState() {
        Reporter.log("Generating random State", LogLevel.INFO_BLUE);
        return faker.address().state();
    }

    /**
     * Generates a random zip code.
     * @return A random zip code
     */
    public static String getRandomZipCode() {
        Reporter.log("Generating random ZipCode", LogLevel.INFO_BLUE);
        return faker.address().zipCode();
    }

    /**
     * Generates a random username.
     * @return A random username
     */
    public static String getRandomUsername() {
        Reporter.log("Generating random Username", LogLevel.INFO_BLUE);
        return faker.name().username();
    }

    /**
     * Generates a random password.
     * @return A random password
     */
    public static String getRandomPassword() {
        Reporter.log("Generating random Password", LogLevel.INFO_BLUE);
        return faker.internet().password();
    }

    /**
     * Generates a random company name.
     * @return A random company name
     */
    public static String getRandomCompany() {
        Reporter.log("Generating random Company", LogLevel.INFO_BLUE);
        return faker.company().name();
    }

    /**
     * Generates a random job title.
     * @return A random job title
     */
    public static String getRandomJobTitle() {
        Reporter.log("Generating random JobTitle", LogLevel.INFO_BLUE);
        return faker.job().title();
    }

    /**
     * Generates a random website URL.
     * @return A random website URL
     */
    public static String getRandomWebsite() {
        Reporter.log("Generating random Website", LogLevel.INFO_BLUE);
        return faker.internet().url();
    }

    /**
     * Generates a random IP address.
     * @return A random IP address
     */
    public static String getRandomIPAddress() {
        Reporter.log("Generating random IPAddress", LogLevel.INFO_BLUE);
        return faker.internet().ipV4Address();
    }

    /**
     * Generates a random birth date.
     * @return A random birth date
     */
    public static String getRandomBirthDate() {
        Reporter.log("Generating random BirthDate", LogLevel.INFO_BLUE);
        return faker.date().birthday().toString();
    }

    /**
     * Generates a random credit card number.
     * @return A random credit card number
     */
    public static String getRandomCreditCardNumber() {
        Reporter.log("Generating random CreditCardNumber", LogLevel.INFO_BLUE);
        return faker.finance().creditCard();
    }

    /**
     * Generates a random credit card expiry date.
     * @return A random credit card expiry date
     */
    public static String getRandomCreditCardExpiry() {
        Reporter.log("Generating random CreditCardExpiry", LogLevel.INFO_BLUE);
        return faker.business().creditCardExpiry();
    }

    /**
     * Generates a random medicine name.
     * @return A random medicine name
     */
    public static String getMedicineName() {
        Reporter.log("Generating random MedicineName", LogLevel.INFO_BLUE);
        return faker.medical().medicineName();
    }

    /**
     * Generates a random university name.
     * @return A random university name
     */
    public static String getRandomUniversity() {
        Reporter.log("Generating random University", LogLevel.INFO_BLUE);
        return faker.educator().university();
    }

    /**
     * Generates a random degree name.
     * @return A random degree name
     */
    public static String getRandomDegree() {
        Reporter.log("Generating random Degree", LogLevel.INFO_BLUE);
        return faker.educator().course();
    }

    /**
     * Generates a random animal name.
     * @return A random animal name
     */
    public static String getRandomAnimal() {
        Reporter.log("Generating random Animal", LogLevel.INFO_BLUE);
        return faker.animal().name();
    }

    /**
     * Generates a random color name.
     * @return A random color name
     */
    public static String getRandomColor() {
        Reporter.log("Generating random Color", LogLevel.INFO_BLUE);
        return faker.color().name();
    }

    /**
     * Generates a random book title.
     * @return A random book title
     */
    public static String getRandomBook() {
        Reporter.log("Generating random Book", LogLevel.INFO_BLUE);
        return faker.book().title();
    }

    /**
     * Generates a random sentence.
     * @return A random sentence
     */
    public static String getRandomSentence() {
        Reporter.log("Generating random Sentence", LogLevel.INFO_BLUE);
        return faker.lorem().sentence();
    }

    /**
     * Generates a random paragraph.
     * @return A random paragraph
     */
    public static String getRandomParagraph() {
        Reporter.log("Generating random Paragraph", LogLevel.INFO_BLUE);
        return faker.lorem().paragraph();
    }

    /**
     * Generates a random quote.
     * @return A random quote
     */
    public static String getRandomQuote() {
        Reporter.log("Generating random Quote", LogLevel.INFO_BLUE);
        return faker.harryPotter().quote();
    }

    /**
     * Generates a random country code.
     * @return A random country code
     */
    public static String getRandomCountryCode() {
        Reporter.log("Generating random CountryCode", LogLevel.INFO_BLUE);
        return faker.address().countryCode();
    }

    /**
     * Generates a random building number.
     * @return A random building number
     */
    public static String getRandomBuildingNumber() {
        Reporter.log("Generating random BuildingNumber", LogLevel.INFO_BLUE);
        return faker.address().buildingNumber();
    }

    /**
     * Generates a random street name.
     * @return A random street name
     */
    public static String getRandomStreetName() {
        Reporter.log("Generating random StreetName", LogLevel.INFO_BLUE);
        return faker.address().streetName();
    }

    /**
     * Generates a random street address.
     * @return A random street address
     */
    public static String getRandomStreetAddress() {
        Reporter.log("Generating random StreetAddress", LogLevel.INFO_BLUE);
        return faker.address().streetAddress();
    }

    /**
     * Generates a random latitude.
     * @return A random latitude
     */
    public static String getRandomLatitude() {
        Reporter.log("Generating random Latitude", LogLevel.INFO_BLUE);
        return String.valueOf(faker.address().latitude());
    }

    /**
     * Generates a random longitude.
     * @return A random longitude
     */
    public static String getRandomLongitude() {
        Reporter.log("Generating random Longitude", LogLevel.INFO_BLUE);
        return String.valueOf(faker.address().longitude());
    }

    /**
     * Generates a random time zone.
     * @return A random time zone
     */
    public static String getRandomTimeZone() {
        Reporter.log("Generating random TimeZone", LogLevel.INFO_BLUE);
        return faker.address().timeZone();
    }

    /**
     * Generates a random bank account number.
     * @return A random bank account number
     */
    public static String getRandomBankAccountNumber() {
        Reporter.log("Generating random BankAccountNumber", LogLevel.INFO_BLUE);
        return faker.finance().iban();
    }

    /**
     * Generates a random SWIFT code.
     * @return A random SWIFT code
     */
    public static String getRandomSWIFTCode() {
        Reporter.log("Generating random SWIFTCode", LogLevel.INFO_BLUE);
        return faker.finance().bic();
    }

    /**
     * Generates a random company industry.
     * @return A random company industry
     */
    public static String getRandomCompanyIndustry() {
        Reporter.log("Generating random CompanyIndustry", LogLevel.INFO_BLUE);
        return faker.company().industry();
    }

    /**
     * Generates a random company catch phrase.
     * @return A random company catch phrase
     */
    public static String getRandomCompanyCatchPhrase() {
        Reporter.log("Generating random CompanyCatchPhrase", LogLevel.INFO_BLUE);
        return faker.company().catchPhrase();
    }

    /**
     * Generates a random product name.
     * @return A random product name
     */
    public static String getRandomProductName() {
        Reporter.log("Generating random ProductName", LogLevel.INFO_BLUE);
        return faker.commerce().productName();
    }

    /**
     * Generates a random product price.
     * @return A random product price
     */
    public static String getRandomProductPrice() {
        Reporter.log("Generating random ProductPrice", LogLevel.INFO_BLUE);
        return faker.commerce().price();
    }

    /**
     * Generates a random product material.
     * @return A random product material
     */
    public static String getRandomProductMaterial() {
        Reporter.log("Generating random ProductMaterial", LogLevel.INFO_BLUE);
        return faker.commerce().material();
    }

    /**
     * Generates a random department.
     * @return A random department
     */
    public static String getRandomDepartment() {
        Reporter.log("Generating random Department", LogLevel.INFO_BLUE);
        return faker.commerce().department();
    }

    /**
     * Generates a random currency code.
     * @return A random currency code
     */
    public static String getRandomCurrencyCode() {
        Reporter.log("Generating random CurrencyCode", LogLevel.INFO_BLUE);
        return faker.currency().code();
    }

    /**
     * Generates a random currency name.
     * @return A random currency name
     */
    public static String getRandomCurrencyName() {
        Reporter.log("Generating random CurrencyName", LogLevel.INFO_BLUE);
        return faker.currency().name();
    }

    /**
     * Generates a random country flag emoji.
     * @return A random country flag emoji
     */
    public static String getRandomCountryFlagEmoji() {
        Reporter.log("Generating random CountryFlagEmoji", LogLevel.INFO_BLUE);
        return faker.country().flag();
    }

    /**
     * Generates a random funny name.
     * @return A random funny name
     */
    public static String getRandomFunnyName() {
        Reporter.log("Generating random FunnyName", LogLevel.INFO_BLUE);
        return faker.funnyName().name();
    }
}