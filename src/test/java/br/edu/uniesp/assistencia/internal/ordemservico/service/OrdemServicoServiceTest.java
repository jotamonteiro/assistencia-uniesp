package br.edu.uniesp.assistencia.internal.ordemservico.service;

import br.edu.uniesp.assistencia.internal.cliente.entity.ClienteEntity;
import br.edu.uniesp.assistencia.internal.cliente.repository.ClienteRepository;
import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import br.edu.uniesp.assistencia.internal.equipamento.repository.EquipamentoRepository;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoRequest;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoResponse;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.OrdemServicoEntity;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.StatusOrdemServico;
import br.edu.uniesp.assistencia.internal.ordemservico.repository.OrdemServicoRepository;
import br.edu.uniesp.assistencia.shared.exception.RecursoNaoEncontradoException;
import br.edu.uniesp.assistencia.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemServicoServiceTest {

    @Mock
    private OrdemServicoRepository ordemServicoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private EquipamentoRepository equipamentoRepository;

    private OrdemServicoService service;

    private ClienteEntity cliente;
    private EquipamentoEntity equipamento;

    @BeforeEach
    void setUp() {
        service = new OrdemServicoService(ordemServicoRepository, clienteRepository, equipamentoRepository);

        cliente = cliente(1L);
        equipamento = new EquipamentoEntity("Notebook", "Dell", "Inspiron", "SN123");
        equipamento.associarCliente(cliente);
        ReflectionTestUtils.setField(equipamento, "id", 2L);
    }

    private static ClienteEntity cliente(Long id) {
        ClienteEntity c = new ClienteEntity("Maria", "12345678901", "maria" + id + "@email.com");
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private CriarOrdemServicoRequest request() {
        return new CriarOrdemServicoRequest(1L, 2L, "Notebook não liga após queda de energia.");
    }

    // Cenário 1 + RN03 + RN04 + RN14
    @Test
    void deveAbrirOrdemComSucesso() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipamentoRepository.findById(2L)).thenReturn(Optional.of(equipamento));
        when(ordemServicoRepository.save(any(OrdemServicoEntity.class))).thenAnswer(inv -> {
            OrdemServicoEntity os = inv.getArgument(0);
            ReflectionTestUtils.setField(os, "id", 10L);
            return os;
        });

        CriarOrdemServicoResponse response = service.abrirOrdemServico(request());

        String data = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(response.dataAbertura()).isNotNull();
        assertThat(response.numeroProtocolo()).matches("OS-" + data + "-[0-9A-F]{4}");

        ArgumentCaptor<OrdemServicoEntity> captor = ArgumentCaptor.forClass(OrdemServicoEntity.class);
        verify(ordemServicoRepository).save(captor.capture());
        assertThat(captor.getValue().getHistorico()).hasSize(1);
        var historico = captor.getValue().getHistorico().get(0);
        assertThat(historico.getNovoStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(historico.getStatusAnterior()).isNull();
        assertThat(historico.getResponsavel()).isEqualTo("SISTEMA");
        assertThat(historico.getObservacao()).isNotBlank();
    }

    // Cenário 3
    @Test
    void deveLancarNaoEncontradoQuandoClienteNaoExiste() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abrirOrdemServico(request()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(ordemServicoRepository, never()).save(any());
    }

    // Cenário 3
    @Test
    void deveLancarNaoEncontradoQuandoEquipamentoNaoExiste() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipamentoRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abrirOrdemServico(request()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(ordemServicoRepository, never()).save(any());
    }

    // Cenário 4 - RN01
    @Test
    void deveRejeitarClienteInativo() {
        cliente.inativar();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipamentoRepository.findById(2L)).thenReturn(Optional.of(equipamento));

        assertThatThrownBy(() -> service.abrirOrdemServico(request()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Não é permitido abrir Ordem de Serviço para um cliente inativo.");
        verify(ordemServicoRepository, never()).save(any());
    }

    // Cenário 5 - RN02
    @Test
    void deveRejeitarEquipamentoDeOutroCliente() {
        equipamento.associarCliente(cliente(99L));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipamentoRepository.findById(2L)).thenReturn(Optional.of(equipamento));

        assertThatThrownBy(() -> service.abrirOrdemServico(request()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O equipamento informado não pertence ao cliente indicado.");
        verify(ordemServicoRepository, never()).save(any());
    }
}
