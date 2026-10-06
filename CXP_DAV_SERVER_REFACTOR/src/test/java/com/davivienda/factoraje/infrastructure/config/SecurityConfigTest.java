package com.davivienda.factoraje.infrastructure.config;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import com.davivienda.factoraje.controller.HolidayController;
import com.davivienda.factoraje.controller.ParameterController;
import com.davivienda.factoraje.controller.PayerController;
import com.davivienda.factoraje.controller.SupplierController;
import com.davivienda.factoraje.controller.UploadResourceController;
import com.davivienda.factoraje.controller.UserController;
import com.davivienda.factoraje.domain.constants.Roles;
import com.davivienda.factoraje.dto.payer.PayerSummaryDTOResponse;
import com.davivienda.factoraje.dto.supplier.SupplierBankAccountDTOResponse;
import com.davivienda.factoraje.dto.upload_resource.ResourceFile;
import com.davivienda.factoraje.dto.upload_resource.UploadResourcesDTOResponse;
import com.davivienda.factoraje.infrastructure.security.JwtService;
import com.davivienda.factoraje.service.HolidayService;
import com.davivienda.factoraje.service.ParameterService;
import com.davivienda.factoraje.service.PayerSummaryService;
import com.davivienda.factoraje.service.SupplierBankAccountService;
import com.davivienda.factoraje.service.UploadResourceService;
import com.davivienda.factoraje.service.UserService;

import io.jsonwebtoken.MalformedJwtException;

@WebMvcTest(controllers = { UserController.class, ParameterController.class, HolidayController.class,
        UploadResourceController.class, PayerController.class, SupplierController.class })
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String PDF = "%PDF-1.4";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private SystemParameters systemParameters;
    @MockBean
    private UserService userService;
    @MockBean
    private ParameterService parameterService;
    @MockBean
    private HolidayService holidayService;
    @MockBean
    private UploadResourceService uploadResourceService;
    @MockBean
    private PayerSummaryService payerSummaryService;
    @MockBean
    private SupplierBankAccountService supplierBankAccountService;

    @Test
    void onlyPayerCanReadItsOwnSummary() throws Exception {
        when(payerSummaryService.getOwnSummary())
                .thenReturn(new PayerSummaryDTOResponse(null, "AEROMAN", "06142711011200", "P-01", null));

        mockMvc.perform(get("/api/v1/payers/me/summary").header(HttpHeaders.AUTHORIZATION, bearer(Roles.PAYER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nit").value("06142711011200"));
        mockMvc.perform(get("/api/v1/payers/me/summary").header(HttpHeaders.AUTHORIZATION, bearer(Roles.SUPPLIER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/payers/me/summary").header(HttpHeaders.AUTHORIZATION, bearer(Roles.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminCanReadAnyPayerSummary() throws Exception {
        UUID payerId = UUID.randomUUID();
        when(payerSummaryService.getSummary(payerId))
                .thenReturn(new PayerSummaryDTOResponse(payerId, "AEROMAN", "06142711011200", "P-01", null));

        mockMvc.perform(get("/api/v1/payers/" + payerId + "/summary").header(HttpHeaders.AUTHORIZATION, bearer(Roles.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("AEROMAN"));
        mockMvc.perform(get("/api/v1/payers/" + payerId + "/summary").header(HttpHeaders.AUTHORIZATION, bearer(Roles.PAYER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlySupplierCanReadItsOwnBankAccount() throws Exception {
        when(supplierBankAccountService.getOwnMainAccount()).thenReturn(new SupplierBankAccountDTOResponse("0123456789"));

        mockMvc.perform(get("/api/v1/suppliers/me/bank-account").header(HttpHeaders.AUTHORIZATION, bearer(Roles.SUPPLIER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNumber").value("0123456789"));
        mockMvc.perform(get("/api/v1/suppliers/me/bank-account").header(HttpHeaders.AUTHORIZATION, bearer(Roles.PAYER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/suppliers/me/bank-account").header(HttpHeaders.AUTHORIZATION, bearer(Roles.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/holidays"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("La sesión no es válida o expiró."));

        verifyNoInteractions(holidayService);
    }

    @Test
    void requestWithInvalidTokenIsUnauthorized() throws Exception {
        when(jwtService.extractUsername("corrupto")).thenThrow(new MalformedJwtException("token inválido"));

        mockMvc.perform(get("/api/v1/holidays").header(HttpHeaders.AUTHORIZATION, "Bearer corrupto"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void supplierCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(Roles.SUPPLIER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operación."));

        verifyNoInteractions(userService);
    }

    @Test
    void adminCannotReadSystemParameters() throws Exception {
        mockMvc.perform(get("/api/v1/parameters").header(HttpHeaders.AUTHORIZATION, bearer(Roles.ADMIN)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(parameterService);
    }

    @Test
    void payerCannotReadHolidays() throws Exception {
        mockMvc.perform(get("/api/v1/holidays").header(HttpHeaders.AUTHORIZATION, bearer(Roles.PAYER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadHolidays() throws Exception {
        when(holidayService.getHolidays()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/holidays").header(HttpHeaders.AUTHORIZATION, bearer(Roles.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void payerCanReadUploadResourcesButCannotChangeThem() throws Exception {
        when(uploadResourceService.getResources()).thenReturn(new UploadResourcesDTOResponse(null, null));

        when(uploadResourceService.getManual()).thenReturn(new ResourceFile("Manual.pdf", PDF.getBytes()));

        mockMvc.perform(get("/api/v1/upload-resources").header(HttpHeaders.AUTHORIZATION, bearer(Roles.PAYER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/upload-resources/manual").header(HttpHeaders.AUTHORIZATION, bearer(Roles.PAYER)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment")));
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/upload-resources/manual").file(manualFile())
                        .header(HttpHeaders.AUTHORIZATION, bearer(Roles.PAYER)))
                .andExpect(status().isForbidden());

        verify(uploadResourceService, never()).replaceManual(any());
    }

    @Test
    void supplierCannotReadUploadResources() throws Exception {
        mockMvc.perform(get("/api/v1/upload-resources/template").header(HttpHeaders.AUTHORIZATION, bearer(Roles.SUPPLIER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/upload-resources/manual").header(HttpHeaders.AUTHORIZATION, bearer(Roles.SUPPLIER)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(uploadResourceService);
    }

    @Test
    void adminCanChangeTheManual() throws Exception {
        when(uploadResourceService.replaceManual(any()))
                .thenReturn(new UploadResourcesDTOResponse.ResourceInfo("Manual.pdf", 8, null, "Ana Operadora"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/upload-resources/manual").file(manualFile())
                        .header(HttpHeaders.AUTHORIZATION, bearer(Roles.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("Manual.pdf"));
    }

    private static MockMultipartFile manualFile() {
        return new MockMultipartFile("file", "Manual.pdf", MediaType.APPLICATION_PDF_VALUE, PDF.getBytes());
    }

    @Test
    void unlistedEndpointIsDeniedEvenForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/unlisted").header(HttpHeaders.AUTHORIZATION, bearer(Roles.ADMIN)))
                .andExpect(status().isForbidden());
    }

    private String bearer(String role) {
        String token = "token-" + role;
        String dui = "dui-" + role;
        UserDetails user = User.withUsername(dui).password("n/a").roles(role).build();

        when(jwtService.extractUsername(token)).thenReturn(dui);
        when(userDetailsService.loadUserByUsername(dui)).thenReturn(user);
        when(jwtService.isTokenValid(eq(token), any(UserDetails.class))).thenReturn(true);

        return "Bearer " + token;
    }
}
