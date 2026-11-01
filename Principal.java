import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;

/**
 * Trabalho de Estruturas de Dados Avançadas: árvore binária de busca
 * parcialmente persistente com cópia de nós sob demanda.
 *
 * Todas as atualizações são feitas na versão mais recente. As versões
 * anteriores continuam disponíveis para consultas, sem copiar a árvore toda.
 */
public class Principal {

    /** Guarda um nó com seus campos originais e uma única alteração histórica. */
    private static final class No {
        private static final byte SEM_ALTERACAO = 0;
        private static final byte ALTERA_VALOR = 1;
        private static final byte ALTERA_ESQUERDA = 2;
        private static final byte ALTERA_DIREITA = 3;

        private long valor;
        private No esquerda;
        private No direita;

        // A ligação com o pai só é usada nas atualizações da versão atual.
        // Ela não participa das consultas históricas e, por isso, pode mudar.
        private No pai;

        // Uma alteração com data: suficiente para consulta em tempo constante.
        private byte campoAlterado = SEM_ALTERACAO;
        private int versaoAlteracao;
        private long valorAlterado;
        private No referenciaAlterada;

        private No(long valor) {
            this.valor = valor;
        }
    }

    /** Mantém todas as raízes publicadas e a árvore da versão mais recente. */
    private static final class ArvorePersistente {
        private static final int LIMITE_ATUALIZACOES = 1000;
        private final No[] raizes = new No[LIMITE_ATUALIZACOES + 1];
        private No raizAtual;
        private int ultimaVersao;

        /** Obtém o valor que um nó apresentava numa versão determinada. */
        private long obterValor(No no, int versao) {
            if (no.campoAlterado == No.ALTERA_VALOR
                    && no.versaoAlteracao <= versao) {
                return no.valorAlterado;
            }
            return no.valor;
        }

        /** Obtém o filho esquerdo que era visível na versão informada. */
        private No obterEsquerda(No no, int versao) {
            if (no.campoAlterado == No.ALTERA_ESQUERDA
                    && no.versaoAlteracao <= versao) {
                return no.referenciaAlterada;
            }
            return no.esquerda;
        }

        /** Obtém o filho direito que era visível na versão informada. */
        private No obterDireita(No no, int versao) {
            if (no.campoAlterado == No.ALTERA_DIREITA
                    && no.versaoAlteracao <= versao) {
                return no.referenciaAlterada;
            }
            return no.direita;
        }

        /**
         * Altera um único campo na versão atual.
         *
         * Se a área de alterações estiver livre, basta preencher. Se ela
         * já estiver ocupada, cria uma cópia do nó com os valores
         * atuais e redirecionamos o ponteiro do pai. O redirecionamento pode
         * subir pela árvore, mas seu custo é constante de forma amortizada.
         */
        private No alterarCampo(No no, byte campo, long novoValor,
                               No novaReferencia, int versao) {
            if (no.campoAlterado == No.SEM_ALTERACAO) {
                no.campoAlterado = campo;
                no.versaoAlteracao = versao;
                no.valorAlterado = novoValor;
                no.referenciaAlterada = novaReferencia;

                if (campo != No.ALTERA_VALOR && novaReferencia != null) {
                    novaReferencia.pai = no;
                }
                return no;
            }

            // Conservamos o nó antigo para as versões históricas.
            No copia = new No(obterValor(no, versao));
            copia.esquerda = obterEsquerda(no, versao);
            copia.direita = obterDireita(no, versao);
            copia.pai = no.pai;

            // Somente os pais da árvore atual precisam permanecer atualizados.
            if (copia.esquerda != null) {
                copia.esquerda.pai = copia;
            }
            if (copia.direita != null) {
                copia.direita.pai = copia;
            }

            // Aplicamos a alteração na cópia, deixando sua área livre.
            if (campo == No.ALTERA_VALOR) {
                copia.valor = novoValor;
            } else if (campo == No.ALTERA_ESQUERDA) {
                copia.esquerda = novaReferencia;
            } else {
                copia.direita = novaReferencia;
            }
            if (campo != No.ALTERA_VALOR && novaReferencia != null) {
                novaReferencia.pai = copia;
            }

            No paiAnterior = no.pai;
            if (paiAnterior == null) {
                raizAtual = copia;
            } else if (obterEsquerda(paiAnterior, versao) == no) {
                alterarCampo(paiAnterior, No.ALTERA_ESQUERDA, 0, copia, versao);
            } else if (obterDireita(paiAnterior, versao) == no) {
                alterarCampo(paiAnterior, No.ALTERA_DIREITA, 0, copia, versao);
            } else {
                throw new IllegalStateException("Ligação com o pai inconsistente");
            }
            return copia;
        }

        /** Publica uma nova versão, inclusive quando nada mudou na árvore. */
        private void publicarVersao() {
            ultimaVersao++;
            raizes[ultimaVersao] = raizAtual;
        }

        /** Procura um valor somente na árvore atual, sem modificar os nós. */
        private No buscarAtual(long valor) {
            No atual = raizAtual;
            while (atual != null) {
                long valorAtual = obterValor(atual, ultimaVersao + 1);
                if (valor == valorAtual) {
                    return atual;
                }
                atual = valor < valorAtual
                        ? obterEsquerda(atual, ultimaVersao + 1)
                        : obterDireita(atual, ultimaVersao + 1);
            }
            return null;
        }

        /** Insere um nó; chaves iguais são inseridas à direita. */
        private void incluir(long valor) {
            conferirCapacidade();
            int proximaVersao = ultimaVersao + 1;
            No novo = new No(valor);

            if (raizAtual == null) {
                raizAtual = novo;
                publicarVersao();
                return;
            }

            No atual = raizAtual;
            while (true) {
                long valorAtual = obterValor(atual, proximaVersao);
                boolean irParaEsquerda = valor < valorAtual;
                No filho = irParaEsquerda
                        ? obterEsquerda(atual, proximaVersao)
                        : obterDireita(atual, proximaVersao);

                if (filho == null) {
                    byte campo = irParaEsquerda
                            ? No.ALTERA_ESQUERDA : No.ALTERA_DIREITA;
                    alterarCampo(atual, campo, 0, novo, proximaVersao);
                    break;
                }
                atual = filho;
            }
            publicarVersao();
        }

        /**
         * Substitui um nó por seu único filho (ou por uma referência vazia).
         * A substituição modifica no máximo uma ligação da árvore.
         */
        private void substituirNo(No antigo, No substituto, int versao) {
            No pai = antigo.pai;
            if (pai == null) {
                raizAtual = substituto;
                if (substituto != null) {
                    substituto.pai = null;
                }
            } else if (obterEsquerda(pai, versao) == antigo) {
                alterarCampo(pai, No.ALTERA_ESQUERDA, 0, substituto, versao);
            } else if (obterDireita(pai, versao) == antigo) {
                alterarCampo(pai, No.ALTERA_DIREITA, 0, substituto, versao);
            } else {
                throw new IllegalStateException("Nó removido sem ligação com o pai");
            }
        }

        /**
         * Remove uma ocorrência. Mesmo quando o valor não existe, cria versão.
         * Com dois filhos, copia-se o valor do sucessor e retira-se aquele nó.
         */
        private void remover(long valor) {
            conferirCapacidade();
            int proximaVersao = ultimaVersao + 1;
            No alvo = buscarAtual(valor);

            if (alvo == null) {
                publicarVersao();
                return;
            }

            No esquerda = obterEsquerda(alvo, proximaVersao);
            No direita = obterDireita(alvo, proximaVersao);

            if (esquerda == null || direita == null) {
                substituirNo(alvo, esquerda != null ? esquerda : direita,
                        proximaVersao);
            } else {
                No sucessor = direita;
                while (obterEsquerda(sucessor, proximaVersao) != null) {
                    sucessor = obterEsquerda(sucessor, proximaVersao);
                }

                long valorSucessor = obterValor(sucessor, proximaVersao);
                // Atualizar o valor antes de desanexar evita perder a ligação
                // com o sucessor quando uma cópia de nó for necessária.
                alterarCampo(alvo, No.ALTERA_VALOR, valorSucessor,
                        null, proximaVersao);
                substituirNo(sucessor, obterDireita(sucessor, proximaVersao),
                        proximaVersao);
            }
            publicarVersao();
        }

        /** Usa a versão recente para identificadores de versão inexistentes. */
        private int resolverVersao(int versao) {
            return versao >= 0 && versao <= ultimaVersao
                    ? versao : ultimaVersao;
        }

        /** Devolve o menor valor estritamente maior, ou -1 caso não exista. */
        private long sucessor(long valor, int versaoSolicitada) {
            int versao = resolverVersao(versaoSolicitada);
            No atual = raizes[versao];
            long resposta = -1;

            while (atual != null) {
                long valorAtual = obterValor(atual, versao);
                if (valorAtual > valor) {
                    resposta = valorAtual;
                    atual = obterEsquerda(atual, versao);
                } else {
                    atual = obterDireita(atual, versao);
                }
            }
            return resposta;
        }

        /** Imprime a versão solicitada, ordenada e sem espaços extras. */
        private void imprimir(int versaoSolicitada, BufferedWriter saida)
                throws IOException {
            int versao = resolverVersao(versaoSolicitada);
            ArrayDeque<No> pilha = new ArrayDeque<>();
            No atual = raizes[versao];
            boolean primeiro = true;

            while (atual != null || !pilha.isEmpty()) {
                while (atual != null) {
                    pilha.push(atual);
                    atual = obterEsquerda(atual, versao);
                }

                atual = pilha.pop();
                if (!primeiro) {
                    saida.write(", ");
                }
                saida.write(Long.toString(obterValor(atual, versao)));
                primeiro = false;
                atual = obterDireita(atual, versao);
            }
            saida.newLine();
        }

        private void conferirCapacidade() {
            if (ultimaVersao == LIMITE_ATUALIZACOES) {
                throw new IllegalArgumentException("Limite de 1000 atualizações excedido");
            }
        }
    }

    /** Interpreta um arquivo com comandos INC, REM, SUC e IMP. */
    private static void executar(BufferedReader entrada, BufferedWriter saida)
            throws IOException {
        ArvorePersistente arvore = new ArvorePersistente();
        String linha;

        while ((linha = entrada.readLine()) != null) {
            String comandoOriginal = linha.trim();
            if (comandoOriginal.isEmpty()) {
                continue;
            }

            String[] partes = comandoOriginal.split("\\s+");
            String comando = partes[0];
            switch (comando) {
                case "INC": {
                    long valor = Long.parseLong(partes[1]);
                    validarValorInserido(valor);
                    arvore.incluir(valor);
                    break;
                }
                case "REM": {
                    long valor = Long.parseLong(partes[1]);
                    validarValorInserido(valor);
                    arvore.remover(valor);
                    break;
                }
                case "SUC": {
                    long valor = Long.parseLong(partes[1]);
                    int versao = Integer.parseInt(partes[2]);
                    saida.write(comandoOriginal);
                    saida.newLine();
                    long resposta = arvore.sucessor(valor, versao);
                    saida.write(resposta == -1 ? "inf" : Long.toString(resposta));
                    saida.newLine();
                    break;
                }
                case "IMP": {
                    int versao = Integer.parseInt(partes[1]);
                    saida.write(comandoOriginal);
                    saida.newLine();
                    arvore.imprimir(versao, saida);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Comando desconhecido: " + comando);
            }
        }
        saida.flush();
    }

    /** Respeita a restrição do enunciado para valores armazenados nos nós. */
    private static void validarValorInserido(long valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("O valor do nó não pode ser negativo");
        }
    }

    /** Inicia a leitura do arquivo passado pela regra run do Makefile. */
    public static void main(String[] argumentos) throws IOException {
        BufferedWriter saida = new BufferedWriter(
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

        if (argumentos.length == 0) {
            try (BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                executar(entrada, saida);
            }
        } else {
            try (BufferedReader entrada = Files.newBufferedReader(
                    Path.of(argumentos[0]), StandardCharsets.UTF_8)) {
                executar(entrada, saida);
            }
        }
    }
}
