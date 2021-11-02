package com.daos;

import com.entidades.Locacao;

import java.util.List;

public interface LocacaoDAO {

	public void salvar(Locacao locacao);

	public List<Locacao> obterLocacoesPendentes();
}
