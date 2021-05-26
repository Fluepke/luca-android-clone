package de.culture4life.luca.testing.provider.baercode;

import de.culture4life.luca.BuildConfig;
import de.culture4life.luca.testing.TestResult;
import de.culture4life.luca.testing.TestResultParsingException;
import de.culture4life.luca.testing.provider.opentestcheck.OpenTestCheckTestResultProviderTest;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.security.cert.CertificateException;

import androidx.test.runner.AndroidJUnit4;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class BaercodeTestResultProviderTest {

    public static final String TEST_QR_CODE = "AQDYYoRAoFhh0INDoQEBogVMFnI1NzZeP4hdCzyUBFA6HnF//LdNKAVQZFIRi/WfWDgvkxDKHFEhncX1JInQo+47n+3ztTTL0g5PgH3W44ITWYYrWENt1EOFPztiHlLI2uLVtOBeBtK2a4GDRKEBOCOhBFA6HnF//LdNKAVQZFIRi/WfWIQAMHvSF47oSzyfgtBcKnvmOoZMHy81b5X5ro3spLag8wE6bGQ8GSPQHmFsd0qcypv1/9AvlN3NrnXidmPOvyMN5KMBpHceFAn+NWUSn0h00whyipngxhfxkF87DIAAlA7Z4OM8Qv9KGa9QvkOkR83NScsmoM8sSXaIBhaf8ivyMBG8NY4=";
    public static final String VACCINATION_QR_CODE = "AQDYYoRAoFhq0INDoQEBogRQ/J8iPKUgNWz56bQAYJ7W7gVMzlQhxrJxgejMrolUWEFlGpItxX7R7yQ0iBsl5T1apMKWLkylIBHhIsgDqpcXrUxOCYcRl4lN6om0Jct6gr/cNgIBhuiE2/pwXDwXOoVhI4GDRKEBOCOhBFD8nyI8pSA1bPnptABgntbuWIQBi/c1phTOBmFtUsdatXDvr4efTD1E+/DLwC/JygORC7qcbYHs895n8nrtq2xJ2rKqSt1JmDg8Y3bdzOhD2fi/ebQBRtYLJ9y7inJffsCiFWLFhkIdX2OFncDVKigBegIsy4jWzYe7aIpIg2OCMwMcePGbYt6mDbetK5WTCQfVFPg6nFE=";
    private BaercodeTestResultProvider provider;

    @Before
    public void setUp() throws IOException, CertificateException {
        provider = new BaercodeTestResultProvider();
        provider.baercodeBundle = BaercodeBundle.getTestBundle();
        provider.baercodeCertificate = new BaercodeCertificate(BaercodeCertificateTest.getFileContent("ba.crt"));
    }

    @Test
    public void parse_testQrCode_hasCorrectKeyId() {
        Assume.assumeTrue("Bundle is expired and re-downloaded on release builds", BuildConfig.DEBUG);
        provider.parse(TEST_QR_CODE)
                .map(BaercodeTestResult::getBase64KeyId)
                .test()
                .assertValue("Oh5xf/y3TSgFUGRSEYv1nw==");
    }

    @Test
    public void parse_invalidData_fails() {
        provider.parse("not a qr code")
                .test().assertError(TestResultParsingException.class);
    }

    @Test
    public void parse_testQrCode_decryptedValues() {
        Assume.assumeTrue("Bundle is expired and re-downloaded on release builds", BuildConfig.DEBUG);
        BaercodeTestResult testResult = provider.parse(TEST_QR_CODE).blockingGet();
        TestResult result = testResult.getLucaTestResult();
        Assert.assertEquals("Max", result.getFirstName());
        Assert.assertEquals("Mustermann", result.getLastName());
        Assert.assertEquals(946598400, testResult.getDateOfBirth());
        Assert.assertEquals(1, testResult.getDiseaseType());
    }

    @Test(expected = TestResultParsingException.class)
    public void verify_testQrCodeWithChangedSignature_fails() throws Exception {
        Assume.assumeTrue("Bundle is expired and re-downloaded on release builds", BuildConfig.DEBUG);
        BaercodeTestResult testResult = provider.parse(TEST_QR_CODE).blockingGet();
        testResult.coseMessage.signature = "anything that is not valid".getBytes();
        provider.decryptPersonalData(testResult);
    }

    @Test
    @Ignore("Uses network")
    public void downloadBundle_fromWebserver_succeeds() throws IOException {
        BaercodeBundle baercodeBundle = BaercodeTestResultProvider.downloadBundle();
        Assert.assertFalse(baercodeBundle.isExpired());
    }

    @Test
    public void localBundle_isExpired() {
        Assert.assertTrue(provider.baercodeBundle.isExpired());
    }

    @Test
    public void parse_vaccinationQrCode_hasCorrectKeyId() {
        Assume.assumeTrue("Bundle is expired and re-downloaded on release builds", BuildConfig.DEBUG);
        provider.parse(VACCINATION_QR_CODE)
                .map(BaercodeTestResult::getBase64KeyId)
                .test()
                .assertValue("/J8iPKUgNWz56bQAYJ7W7g==");
    }

    @Test
    public void parse_vaccinationQrCode_hasTwoProcedures() {
        Assume.assumeTrue("Bundle is expired and re-downloaded on release builds", BuildConfig.DEBUG);
        provider.parse(VACCINATION_QR_CODE)
                .map(BaercodeTestResult::getProcedures)
                .map(procedures -> procedures.size())
                .test()
                .assertValue(2);
    }

    @Test
    public void canParse_anything_isFalse() {
        provider.canParse("anything")
                .test()
                .assertValue(false);
    }

    @Test
    public void canParse_ticketIo_isFalse() {
        provider.canParse(OpenTestCheckTestResultProviderTest.VALID_TEST_RESULT_TICKET_IO)
                .test()
                .assertValue(false);
    }

    @Test
    public void canParse_baercode_isTrue() {
        provider.canParse(BaercodeTestResultProviderTest.TEST_QR_CODE)
                .test()
                .assertValue(true);
    }
}