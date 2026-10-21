package br.edu.uniesp.assistencia.internal.ordemservico.controller;

import br.edu.uniesp.assistencia.config.exception.GlobalExceptionHandler;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoRequest;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoResponse;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.StatusOrdemServico;
import br.edu.uniesp.assistencia.internal.ordemservico.service.OrdemServicoService;
import br.edu.uniesp.assistencia.shared.exception.RecursoNaoEncontradoException;
import br.edu.uniesp.assistencia.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrdemServicoControllerTest {

    private static final String JSON_VALIDO = """
            {"clienteId": 1, "equipamentoId": 2, "descricaoProblema": "Notebook não liga."}
            """;

    @Mock
    private OrdemServicoService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrdemServicoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions postar(String json) throws Exception {
        return mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // Cenário 1
    @Test
    void deveRetornar201ComDadosDaOrdem() throws Exception {
        when(service.abrirOrdemServico(any(CriarOrdemServicoRequest.class))).thenReturn(
                new CriarOrdemServicoResponse(1L, "OS-20261015-9F2B", StatusOrdemServico.RECEBIDA,
                        LocalDateTime.of(2026, 10, 15, 20, 30)));

        postar(JSON_VALIDO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numeroProtocolo").value("OS-20261015-9F2B"))
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.dataAbertura").exists());
    }

    // Cenário 2
    @Test
    void deveRetornar400ParaCamposNulosOuDescricaoEmBranco() throws Exception {
        postar("""
                {"clienteId": null, "equipamentoId": null, "descricaoProblema": "   "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.clienteId").exists())
                .andExpect(jsonPath("$.erros.equipamentoId").exists())
                .andExpect(jsonPath("$.erros.descricaoProblema").exists());
    }

    @Test
    void deveRetornar400ParaJsonMalformado() throws Exception {
        postar("{ nao eh json").andExpect(status().isBadRequest());
    }

    // Cenário 3
    @Test
    void deveRetornar404QuandoRecursoNaoExiste() throws Exception {
        when(service.abrirOrdemServico(any())).thenThrow(new RecursoNaoEncontradoException("Cliente", 1L));

        postar(JSON_VALIDO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.recurso").value("Cliente"));
    }

    // Cenários 4 e 5
    @Test
    void deveRetornar422QuandoRegraDeNegocioViolada() throws Exception {
        when(service.abrirOrdemServico(any())).thenThrow(
                new RegraNegocioException("O equipamento informado não pertence ao cliente indicado."));

        postar(JSON_VALIDO)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("O equipamento informado não pertence ao cliente indicado."));
    }
}
