package AutoEllithiumSphere.Utilities;

import com.github.javafaker.Faker;
public class TestDataGenerator {
    private static final Faker faker = new Faker();
    public static String getRandomFullName() {
        return faker.name().fullName();
    }
    public static String getRandomFirstName() {
        return faker.name().firstName();
    }
    public static String getRandomLastName() {
        return faker.name().lastName();
    }
    public static String getRandomEmail() {
        return faker.internet().emailAddress();
    }
    public static String getRandomPhoneNumber() {
        return faker.phoneNumber().cellPhone();
    }
    public static String getRandomAddress() {
        return faker.address().fullAddress();
    }
    public static String getRandomCity() {
        return faker.address().city();
    }
    public static String getRandomCountry() {
        return faker.address().country();
    }
    public static String getRandomState() {
        return faker.address().state();
    }
    public static String getRandomZipCode() {
        return faker.address().zipCode();
    }
    public static String getRandomUsername() {
        return faker.name().username();
    }
    public static String getRandomPassword() {
        return faker.internet().password();
    }
    public static String getRandomCompany() {
        return faker.company().name();
    }
    public static String getRandomJobTitle() {
        return faker.job().title();
    }
    public static String getRandomWebsite() {
        return faker.internet().url();
    }
    public static String getRandomIPAddress() {
        return faker.internet().ipV4Address();
    }
    public static String getRandomBirthDate() {
        return faker.date().birthday().toString();
    }
    public static String getRandomCreditCardNumber() {
        return faker.finance().creditCard();
    }
    public static String getRandomCreditCardExpiry() {
        return faker.business().creditCardExpiry();
    }
    public static String getMedicineName() {
        return faker.medical().medicineName();
    }
    public static String getRandomUniversity() {
        return faker.educator().university();
    }
    public static String getRandomDegree() {
        return faker.educator().course();
    }
    public static String getRandomAnimal() {
        return faker.animal().name();
    }
    public static String getRandomColor() {
        return faker.color().name();
    }
    public static String getRandomBook() {
        return faker.book().title();
    }
    public static String getRandomSentence() {
        return faker.lorem().sentence();
    }
    public static String getRandomParagraph() {
        return faker.lorem().paragraph();
    }
    public static String getRandomQuote() {
        return faker.harryPotter().quote();
    }
    public static String getRandomCountryCode() {
        return faker.address().countryCode();
    }
    public static String getRandomBuildingNumber() {
        return faker.address().buildingNumber();
    }
    public static String getRandomStreetName() {
        return faker.address().streetName();
    }
    public static String getRandomStreetAddress() {
        return faker.address().streetAddress();
    }
    public static String getRandomLatitude() {
        return String.valueOf(faker.address().latitude());
    }
    public static String getRandomLongitude() {
        return String.valueOf(faker.address().longitude());
    }
    public static String getRandomTimeZone() {
        return faker.address().timeZone();
    }
    public static String getRandomBankAccountNumber() {
        return faker.finance().iban();
    }
    public static String getRandomSWIFTCode() {
        return faker.finance().bic();
    }
    public static String getRandomCompanyIndustry() {
        return faker.company().industry();
    }
    public static String getRandomCompanyCatchPhrase() {
        return faker.company().catchPhrase();
    }
    public static String getRandomProductName() {
        return faker.commerce().productName();
    }
    public static String getRandomProductPrice() {
        return faker.commerce().price();
    }
    public static String getRandomProductMaterial() {
        return faker.commerce().material();
    }
    public static String getRandomDepartment() {
        return faker.commerce().department();
    }
    public static String getRandomCurrencyCode() {
        return faker.currency().code();
    }
    public static String getRandomCurrencyName() {
        return faker.currency().name();
    }
    public static String getRandomCountryFlagEmoji() {
        return faker.country().flag();
    }
    public static String getRandomFunnyName() {
        return faker.funnyName().name();
    }
}