package ch.admin.bag.covidcertificate.service;

import ch.admin.bag.covidcertificate.api.exception.CreateCertificateException;
import ch.admin.bag.covidcertificate.api.request.RecoveryCertificateCreateDto;
import ch.admin.bag.covidcertificate.api.request.TestCertificateCreateDto;
import ch.admin.bag.covidcertificate.api.request.VaccinationCertificateCreateDto;
import ch.admin.bag.covidcertificate.service.document.CovidPdfCertificateGenerationService;
import ch.admin.bag.covidcertificate.service.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.flextrade.jfixture.JFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import se.digg.dgc.encoding.Barcode;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class CovidCertificateGenerationServiceTest {
    @InjectMocks
    private CovidCertificateGenerationService service;

    @Mock
    private BarcodeService barcodeService;
    @Mock
    private CovidPdfCertificateGenerationService covidPdfCertificateGenerationService;
    @Mock
    private CovidCertificateDtoMapperService covidCertificateDtoMapperService;
    @Mock
    private ObjectMapper objectMapper;

    private final JFixture fixture = new JFixture();

    @BeforeEach
    public void setUp() throws IOException {
        lenient().when(barcodeService.createBarcode(any())).thenReturn(fixture.create(Barcode.class));
        lenient().when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any())).thenReturn(fixture.create(byte[].class));

        lenient().when(covidCertificateDtoMapperService.toVaccinationCertificateQrCode(any())).thenReturn(fixture.create(VaccinationCertificateQrCode.class));
        lenient().when(covidCertificateDtoMapperService.toVaccinationCertificatePdf(any(), any())).thenReturn(fixture.create(VaccinationCertificatePdf.class));
        lenient().when(covidCertificateDtoMapperService.toTestCertificateQrCode(any())).thenReturn(fixture.create(TestCertificateQrCode.class));
        lenient().when(covidCertificateDtoMapperService.toTestCertificatePdf(any(), any())).thenReturn(fixture.create(TestCertificatePdf.class));
        lenient().when(covidCertificateDtoMapperService.toRecoveryCertificateQrCode(any())).thenReturn(fixture.create(RecoveryCertificateQrCode.class));
        lenient().when(covidCertificateDtoMapperService.toRecoveryCertificatePdf(any(), any())).thenReturn(fixture.create(RecoveryCertificatePdf.class));

        ObjectWriter objectWriter = mock(ObjectWriter.class);
        lenient().when(objectMapper.writer()).thenReturn(objectWriter);
        lenient().when(objectWriter.writeValueAsString(any())).thenReturn(fixture.create(String.class));
    }

    @Nested
    class GenerateVaccinationCovidCertificate {
        @Test
        void shouldMapDtoToVaccinationCertificateQrCode() throws IOException {
            var createDto = fixture.create(VaccinationCertificateCreateDto.class);
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toVaccinationCertificateQrCode(createDto);
        }

        @Test
        void shouldMapDtoToVaccinationCertificatePdf() throws IOException {
            var createDto = fixture.create(VaccinationCertificateCreateDto.class);
            var qrCodeData = fixture.create(VaccinationCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toVaccinationCertificateQrCode(createDto)).thenReturn(qrCodeData);
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toVaccinationCertificatePdf(createDto, qrCodeData);
        }

        @Test
        void throwsCreateCertificateException_ifMapDtoToVaccinationCertificateQrCodeThrowsCreateCertificateException() {
            var createDto = fixture.create(VaccinationCertificateCreateDto.class);
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toVaccinationCertificateQrCode(any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void throwsInvalidCountryOfVaccination_ifMapDtoToVaccinationCertificatePdfThrowsCreateCertificateException() {
            var createDto = fixture.create(VaccinationCertificateCreateDto.class);
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toVaccinationCertificatePdf(any(), any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void shouldCreateBarcode() throws IOException {
            var createDto = fixture.create(VaccinationCertificateCreateDto.class);
            var qrCodeData = fixture.create(VaccinationCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toVaccinationCertificateQrCode(createDto)).thenReturn(qrCodeData);
            var contents = fixture.create(String.class);
            var objectWriter = mock(ObjectWriter.class);
            when(objectMapper.writer()).thenReturn(objectWriter);
            when(objectWriter.writeValueAsString(qrCodeData)).thenReturn(contents);

            service.generateCovidCertificate(createDto);
            
            verify(barcodeService).createBarcode(contents);
        }

        @Test
        void shouldCreatePdf() throws IOException {
            var createDto = fixture.create(VaccinationCertificateCreateDto.class);
            var vaccinationPdf = fixture.create(VaccinationCertificatePdf.class);
            var barcode = fixture.create(Barcode.class);
            when(covidCertificateDtoMapperService.toVaccinationCertificatePdf(any(), any())).thenReturn(vaccinationPdf);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            service.generateCovidCertificate(createDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(vaccinationPdf, barcode);
        }

        @Test
        void shouldReturnBarcode() throws IOException {
            var createDto = fixture.create(VaccinationCertificateCreateDto.class);
            var barcode = fixture.create(Barcode.class);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(barcode.getImage(), actual.getQrCode());
        }

        @Test
        void shouldReturnPdf() throws IOException {
            var createDto = fixture.create(VaccinationCertificateCreateDto.class);
            var pdf = fixture.create(byte[].class);
            when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any())).thenReturn(pdf);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(pdf, actual.getPdf());
        }

        @Test
        void shouldUVCI() throws IOException {
            var createDto = fixture.create(VaccinationCertificateCreateDto.class);

            var actual = service.generateCovidCertificate(createDto);

            assertNotNull(actual.getUvci());
        }
    }

    @Nested
    class GenerateTestCovidCertificate {
        @Test
        void shouldMapDtoToTestCertificateQrCode() throws IOException {
            var createDto = fixture.create(TestCertificateCreateDto.class);
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toTestCertificateQrCode(createDto);
        }

        @Test
        void shouldMapDtoToTestCertificatePdf() throws IOException {
            var createDto = fixture.create(TestCertificateCreateDto.class);
            var qrCodeData = fixture.create(TestCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toTestCertificateQrCode(createDto)).thenReturn(qrCodeData);
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toTestCertificatePdf(createDto, qrCodeData);
        }

        @Test
        void throwsCreateCertificateException_ifMapDtoToTestCertificateQrCodeThrowsCreateCertificateException() {
            var createDto = fixture.create(TestCertificateCreateDto.class);
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toTestCertificateQrCode(any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void throwsInvalidCountryOfTest_ifMapDtoToTestCertificatePdfThrowsCreateCertificateException() {
            var createDto = fixture.create(TestCertificateCreateDto.class);
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toTestCertificatePdf(any(), any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void shouldCreateBarcode() throws IOException {
            var createDto = fixture.create(TestCertificateCreateDto.class);
            var qrCodeData = fixture.create(TestCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toTestCertificateQrCode(createDto)).thenReturn(qrCodeData);
            var contents = fixture.create(String.class);
            var objectWriter = mock(ObjectWriter.class);
            when(objectMapper.writer()).thenReturn(objectWriter);
            when(objectWriter.writeValueAsString(qrCodeData)).thenReturn(contents);

            service.generateCovidCertificate(createDto);

            verify(barcodeService).createBarcode(contents);
        }

        @Test
        void shouldCreatePdf() throws IOException {
            var createDto = fixture.create(TestCertificateCreateDto.class);
            var TestPdf = fixture.create(TestCertificatePdf.class);
            var barcode = fixture.create(Barcode.class);
            when(covidCertificateDtoMapperService.toTestCertificatePdf(any(), any())).thenReturn(TestPdf);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            service.generateCovidCertificate(createDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(TestPdf, barcode);
        }

        @Test
        void shouldReturnBarcode() throws IOException {
            var createDto = fixture.create(TestCertificateCreateDto.class);
            var barcode = fixture.create(Barcode.class);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(barcode.getImage(), actual.getQrCode());
        }

        @Test
        void shouldReturnPdf() throws IOException {
            var createDto = fixture.create(TestCertificateCreateDto.class);
            var pdf = fixture.create(byte[].class);
            when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any())).thenReturn(pdf);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(pdf, actual.getPdf());
        }

        @Test
        void shouldUVCI() throws IOException {
            var createDto = fixture.create(TestCertificateCreateDto.class);

            var actual = service.generateCovidCertificate(createDto);

            assertNotNull(actual.getUvci());
        }
    }

    @Nested
    class GenerateRecoveryCovidCertificate {
        @Test
        void shouldMapDtoToRecoveryCertificateQrCode() throws IOException {
            var createDto = fixture.create(RecoveryCertificateCreateDto.class);
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toRecoveryCertificateQrCode(createDto);
        }

        @Test
        void shouldMapDtoToRecoveryCertificatePdf() throws IOException {
            var createDto = fixture.create(RecoveryCertificateCreateDto.class);
            var qrCodeData = fixture.create(RecoveryCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toRecoveryCertificateQrCode(createDto)).thenReturn(qrCodeData);
            service.generateCovidCertificate(createDto);
            verify(covidCertificateDtoMapperService).toRecoveryCertificatePdf(createDto, qrCodeData);
        }

        @Test
        void throwsCreateCertificateException_ifMapDtoToRecoveryCertificateQrCodeThrowsCreateCertificateException() {
            var createDto = fixture.create(RecoveryCertificateCreateDto.class);
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toRecoveryCertificateQrCode(any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void throwsInvalidCountryOfRecovery_ifMapDtoToRecoveryCertificatePdfThrowsCreateCertificateException() {
            var createDto = fixture.create(RecoveryCertificateCreateDto.class);
            var expected = fixture.create(CreateCertificateException.class);
            when(covidCertificateDtoMapperService.toRecoveryCertificatePdf(any(), any())).thenThrow(expected);

            CreateCertificateException exception = assertThrows(CreateCertificateException.class, () -> service.generateCovidCertificate(createDto));

            assertEquals(expected.getError(), exception.getError());
        }

        @Test
        void shouldCreateBarcode() throws IOException {
            var createDto = fixture.create(RecoveryCertificateCreateDto.class);
            var qrCodeData = fixture.create(RecoveryCertificateQrCode.class);
            when(covidCertificateDtoMapperService.toRecoveryCertificateQrCode(createDto)).thenReturn(qrCodeData);
            var contents = fixture.create(String.class);
            var objectWriter = mock(ObjectWriter.class);
            when(objectMapper.writer()).thenReturn(objectWriter);
            when(objectWriter.writeValueAsString(qrCodeData)).thenReturn(contents);

            service.generateCovidCertificate(createDto);

            verify(barcodeService).createBarcode(contents);
        }

        @Test
        void shouldCreatePdf() throws IOException {
            var createDto = fixture.create(RecoveryCertificateCreateDto.class);
            var RecoveryPdf = fixture.create(RecoveryCertificatePdf.class);
            var barcode = fixture.create(Barcode.class);
            when(covidCertificateDtoMapperService.toRecoveryCertificatePdf(any(), any())).thenReturn(RecoveryPdf);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            service.generateCovidCertificate(createDto);

            verify(covidPdfCertificateGenerationService).generateCovidCertificate(RecoveryPdf, barcode);
        }

        @Test
        void shouldReturnBarcode() throws IOException {
            var createDto = fixture.create(RecoveryCertificateCreateDto.class);
            var barcode = fixture.create(Barcode.class);
            when(barcodeService.createBarcode(any())).thenReturn(barcode);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(barcode.getImage(), actual.getQrCode());
        }

        @Test
        void shouldReturnPdf() throws IOException {
            var createDto = fixture.create(RecoveryCertificateCreateDto.class);
            var pdf = fixture.create(byte[].class);
            when(covidPdfCertificateGenerationService.generateCovidCertificate(any(), any())).thenReturn(pdf);

            var actual = service.generateCovidCertificate(createDto);

            assertEquals(pdf, actual.getPdf());
        }

        @Test
        void shouldUVCI() throws IOException {
            var createDto = fixture.create(RecoveryCertificateCreateDto.class);

            var actual = service.generateCovidCertificate(createDto);

            assertNotNull(actual.getUvci());
        }
    }
}
