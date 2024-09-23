package Ellithium.Utilities;

import com.github.javafaker.Faker;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import java.text.SimpleDateFormat;
import java.util.Date;
public class TestDataGenerator {
    private static final Faker faker = new Faker();

    public static String getRandomFullName() {
        logsUtils.info(Colors.PURPLE + "Generating random FullName" + Colors.RESET);
        return faker.name().fullName();
    }
    public static String getTimeStamp() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-h-m-ssa").format(new Date());
        logsUtils.info(Colors.BLUE+"Getting Timestamp: " + timestamp+Colors.RESET);
        Allure.step("Getting Timestamp: " + timestamp, Status.PASSED);
        return timestamp;
    }
    public static String getRandomFirstName() {
        logsUtils.info(Colors.PURPLE + "Generating random FirstName" + Colors.RESET);
        return faker.name().firstName();
    }

    public static String getRandomLastName() {
        logsUtils.info(Colors.PURPLE + "Generating random LastName" + Colors.RESET);
        return faker.name().lastName();
    }

    public static String getRandomEmail() {
        logsUtils.info(Colors.PURPLE + "Generating random Email" + Colors.RESET);
        return faker.internet().emailAddress();
    }

    public static String getRandomPhoneNumber() {
        logsUtils.info(Colors.PURPLE + "Generating random PhoneNumber" + Colors.RESET);
        return faker.phoneNumber().cellPhone();
    }

    public static String getRandomAddress() {
        logsUtils.info(Colors.PURPLE + "Generating random Address" + Colors.RESET);
        return faker.address().fullAddress();
    }

    public static String getRandomCity() {
        logsUtils.info(Colors.PURPLE + "Generating random City" + Colors.RESET);
        return faker.address().city();
    }

    public static String getRandomCountry() {
        logsUtils.info(Colors.PURPLE + "Generating random Country" + Colors.RESET);
        return faker.address().country();
    }

    public static String getRandomState() {
        logsUtils.info(Colors.PURPLE + "Generating random State" + Colors.RESET);
        return faker.address().state();
    }

    public static String getRandomZipCode() {
        logsUtils.info(Colors.PURPLE + "Generating random ZipCode" + Colors.RESET);
        return faker.address().zipCode();
    }

    public static String getRandomUsername() {
        logsUtils.info(Colors.PURPLE + "Generating random Username" + Colors.RESET);
        return faker.name().username();
    }

    public static String getRandomPassword() {
        logsUtils.info(Colors.PURPLE + "Generating random Password" + Colors.RESET);
        return faker.internet().password();
    }

    public static String getRandomCompany() {
        logsUtils.info(Colors.PURPLE + "Generating random Company" + Colors.RESET);
        return faker.company().name();
    }

    public static String getRandomJobTitle() {
        logsUtils.info(Colors.PURPLE + "Generating random JobTitle" + Colors.RESET);
        return faker.job().title();
    }

    public static String getRandomWebsite() {
        logsUtils.info(Colors.PURPLE + "Generating random Website" + Colors.RESET);
        return faker.internet().url();
    }

    public static String getRandomIPAddress() {
        logsUtils.info(Colors.PURPLE + "Generating random IPAddress" + Colors.RESET);
        return faker.internet().ipV4Address();
    }

    public static String getRandomBirthDate() {
        logsUtils.info(Colors.PURPLE + "Generating random BirthDate" + Colors.RESET);
        return faker.date().birthday().toString();
    }

    public static String getRandomCreditCardNumber() {
        logsUtils.info(Colors.PURPLE + "Generating random CreditCardNumber" + Colors.RESET);
        return faker.finance().creditCard();
    }

    public static String getRandomCreditCardExpiry() {
        logsUtils.info(Colors.PURPLE + "Generating random CreditCardExpiry" + Colors.RESET);
        return faker.business().creditCardExpiry();
    }

    public static String getMedicineName() {
        logsUtils.info(Colors.PURPLE + "Generating random MedicineName" + Colors.RESET);
        return faker.medical().medicineName();
    }

    public static String getRandomUniversity() {
        logsUtils.info(Colors.PURPLE + "Generating random University" + Colors.RESET);
        return faker.educator().university();
    }

    public static String getRandomDegree() {
        logsUtils.info(Colors.PURPLE + "Generating random Degree" + Colors.RESET);
        return faker.educator().course();
    }

    public static String getRandomAnimal() {
        logsUtils.info(Colors.PURPLE + "Generating random Animal" + Colors.RESET);
        return faker.animal().name();
    }

    public static String getRandomColor() {
        logsUtils.info(Colors.PURPLE + "Generating random Color" + Colors.RESET);
        return faker.color().name();
    }

    public static String getRandomBook() {
        logsUtils.info(Colors.PURPLE + "Generating random Book" + Colors.RESET);
        return faker.book().title();
    }

    public static String getRandomSentence() {
        logsUtils.info(Colors.PURPLE + "Generating random Sentence" + Colors.RESET);
        return faker.lorem().sentence();
    }

    public static String getRandomParagraph() {
        logsUtils.info(Colors.PURPLE + "Generating random Paragraph" + Colors.RESET);
        return faker.lorem().paragraph();
    }

    public static String getRandomQuote() {
        logsUtils.info(Colors.PURPLE + "Generating random Quote" + Colors.RESET);
        return faker.harryPotter().quote();
    }

    public static String getRandomCountryCode() {
        logsUtils.info(Colors.PURPLE + "Generating random CountryCode" + Colors.RESET);
        return faker.address().countryCode();
    }

    public static String getRandomBuildingNumber() {
        logsUtils.info(Colors.PURPLE + "Generating random BuildingNumber" + Colors.RESET);
        return faker.address().buildingNumber();
    }

    public static String getRandomStreetName() {
        logsUtils.info(Colors.PURPLE + "Generating random StreetName" + Colors.RESET);
        return faker.address().streetName();
    }

    public static String getRandomStreetAddress() {
        logsUtils.info(Colors.PURPLE + "Generating random StreetAddress" + Colors.RESET);
        return faker.address().streetAddress();
    }

    public static String getRandomLatitude() {
        logsUtils.info(Colors.PURPLE + "Generating random Latitude" + Colors.RESET);
        return String.valueOf(faker.address().latitude());
    }

    public static String getRandomLongitude() {
        logsUtils.info(Colors.PURPLE + "Generating random Longitude" + Colors.RESET);
        return String.valueOf(faker.address().longitude());
    }

    public static String getRandomTimeZone() {
        logsUtils.info(Colors.PURPLE + "Generating random TimeZone" + Colors.RESET);
        return faker.address().timeZone();
    }

    public static String getRandomBankAccountNumber() {
        logsUtils.info(Colors.PURPLE + "Generating random BankAccountNumber" + Colors.RESET);
        return faker.finance().iban();
    }

    public static String getRandomSWIFTCode() {
        logsUtils.info(Colors.PURPLE + "Generating random SWIFTCode" + Colors.RESET);
        return faker.finance().bic();
    }

    public static String getRandomCompanyIndustry() {
        logsUtils.info(Colors.PURPLE + "Generating random CompanyIndustry" + Colors.RESET);
        return faker.company().industry();
    }

    public static String getRandomCompanyCatchPhrase() {
        logsUtils.info(Colors.PURPLE + "Generating random CompanyCatchPhrase" + Colors.RESET);
        return faker.company().catchPhrase();
    }

    public static String getRandomProductName() {
        logsUtils.info(Colors.PURPLE + "Generating random ProductName" + Colors.RESET);
        return faker.commerce().productName();
    }

    public static String getRandomProductPrice() {
        logsUtils.info(Colors.PURPLE + "Generating random ProductPrice" + Colors.RESET);
        return faker.commerce().price();
    }

    public static String getRandomProductMaterial() {
        logsUtils.info(Colors.PURPLE + "Generating random ProductMaterial" + Colors.RESET);
        return faker.commerce().material();
    }

    public static String getRandomDepartment() {
        logsUtils.info(Colors.PURPLE + "Generating random Department" + Colors.RESET);
        return faker.commerce().department();
    }

    public static String getRandomCurrencyCode() {
        logsUtils.info(Colors.PURPLE + "Generating random CurrencyCode" + Colors.RESET);
        return faker.currency().code();
    }

    public static String getRandomCurrencyName() {
        logsUtils.info(Colors.PURPLE + "Generating random CurrencyName" + Colors.RESET);
        return faker.currency().name();
    }

    public static String getRandomCountryFlagEmoji() {
        logsUtils.info(Colors.PURPLE + "Generating random CountryFlagEmoji" + Colors.RESET);
        return faker.country().flag();
    }

    public static String getRandomFunnyName() {
        logsUtils.info(Colors.PURPLE + "Generating random FunnyName" + Colors.RESET);
        return faker.funnyName().name();
    }
}