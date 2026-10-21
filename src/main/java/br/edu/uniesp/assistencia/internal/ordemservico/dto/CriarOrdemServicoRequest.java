package br.edu.uniesp.assistencia.internal.ordemservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarOrdemServicoRequest(

        @NotNull(message = "O ID do cliente é obrigatório")
        Long clienteId,

        @NotNull(message = "O ID do equipamento é obrigatório")
        Long equipamentoId,

        @NotBlank(message = "A descrição do problema é obrigatória")
        String descricaoProblema
) {
}
