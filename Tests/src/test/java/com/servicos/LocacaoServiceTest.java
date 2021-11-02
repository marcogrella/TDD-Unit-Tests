package com.servicos;


import com.builders.FilmeBuilder;
import com.builders.LocacaoBuilder;
import com.builders.UsuarioBuilder;
import com.daos.LocacaoDAO;
import com.entidades.Filme;
import com.entidades.Locacao;
import com.entidades.Usuario;
import com.exceptions.FilmeSemEstoqueException;
import com.exceptions.LocadoraException;
import com.matchers.MatchersProprios;
import com.runners.ParallelRunner;
import com.utils.DataUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


@RunWith(ParallelRunner.class)
public class LocacaoServiceTest {

	@InjectMocks @Spy
	private LocacaoService service;
	
	@Mock
	private SPCService spc;
	@Mock
	private LocacaoDAO dao;
	@Mock
	private EmailService email;
	
	@Rule
	public ErrorCollector error = new ErrorCollector();
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setup(){
		MockitoAnnotations.initMocks(this);
		System.out.println("Iniciando");
	}

	@Before
	public void tearDown(){
		System.out.println("Finalizando...");
	}
	
	@Test
	public void deveAlugarFilme() throws Exception {
		//cenario
		Usuario usuario = UsuarioBuilder.umUsuario().agora();
		List<Filme> filmes = Arrays.asList(FilmeBuilder.umFilme().comValor(5.0).agora());

		// utilizamos o Mockito para chamar um método protegido
		Mockito.doReturn(DataUtils.obterData(5, 11, 2021)).when(service).obterData();

		//acao
		Locacao locacao = service.alugarFilme(usuario, filmes);
			
		//verificacao
		error.checkThat(locacao.getValor(), is(equalTo(5.0)));
		//error.checkThat(locacao.getDataLocacao(), MatchersProprios.ehHoje());
		//error.checkThat(locacao.getDataRetorno(), MatchersProprios.ehHojeComDiferencaDias(1));
		error.checkThat(DataUtils.isMesmaData(locacao.getDataLocacao(), DataUtils.obterData(5, 11, 2021)), is(true));
	}
	
	@Test(expected = FilmeSemEstoqueException.class)
	public void naoDeveAlugarFilmeSemEstoque() throws Exception{
		//cenario
		Usuario usuario = UsuarioBuilder.umUsuario().agora();
		List<Filme> filmes = Arrays.asList(FilmeBuilder.umFilmeSemEstoque().agora());
		
		//acao
		service.alugarFilme(usuario, filmes);
	}
	
	@Test
	public void naoDeveAlugarFilmeSemUsuario() throws FilmeSemEstoqueException{
		//cenario
		List<Filme> filmes = Arrays.asList(FilmeBuilder.umFilme().agora());
		
		//acao
		try {
			service.alugarFilme(null, filmes);
			Assert.fail();
		} catch (LocadoraException e) {
			assertThat(e.getMessage(), is("Usuario vazio"));
		}
	}

	@Test
	public void naoDeveAlugarFilmeSemFilme() throws FilmeSemEstoqueException, LocadoraException{
		//cenario
		Usuario usuario = UsuarioBuilder.umUsuario().agora();
		
		exception.expect(LocadoraException.class);
		exception.expectMessage("Filme vazio");
		
		//acao
		service.alugarFilme(usuario, null);
	}
	
	@Test
	public void deveDevolverNaSegundaAoAlugarNoSabado() throws Exception{
		//cenario
		Usuario usuario = UsuarioBuilder.umUsuario().agora();
		List<Filme> filmes = Arrays.asList(FilmeBuilder.umFilme().agora());


		Mockito.doReturn(DataUtils.obterData(6, 11, 2021)).when(service).obterData();


		//acao
		Locacao retorno = service.alugarFilme(usuario, filmes);
		
		// verificacao
		assertThat(retorno.getDataRetorno(), MatchersProprios.caiNumaSegunda());

	}
	
	@Test
	public void naoDeveAlugarFilmeParaNegativadoSPC() throws Exception {
		//cenario
		Usuario usuario = UsuarioBuilder.umUsuario().agora();
		List<Filme> filmes = Arrays.asList(FilmeBuilder.umFilme().agora());
		
		when(spc.possuiNegativacao(Mockito.any(Usuario.class))).thenReturn(true);
		
		//acao
		try {
			service.alugarFilme(usuario, filmes);
		//verificacao
			Assert.fail();
		} catch (LocadoraException e) {
			Assert.assertThat(e.getMessage(), is("Usuário Negativado"));
		}
		
		verify(spc).possuiNegativacao(usuario);
	}
	
	@Test
	public void deveEnviarEmailParaLocacoesAtrasadas(){
		//cenario
		Usuario usuario = UsuarioBuilder.umUsuario().agora();
		Usuario usuario2 = UsuarioBuilder.umUsuario().comNome("Usuario em dia").agora();
		Usuario usuario3 = UsuarioBuilder.umUsuario().comNome("Outro atrasado").agora();
		List<Locacao> locacoes = Arrays.asList(
				LocacaoBuilder.umLocacao().atrasada().comUsuario(usuario).agora(),
				LocacaoBuilder.umLocacao().comUsuario(usuario2).agora(),
				LocacaoBuilder.umLocacao().atrasada().comUsuario(usuario3).agora(),
				LocacaoBuilder.umLocacao().atrasada().comUsuario(usuario3).agora());
		when(dao.obterLocacoesPendentes()).thenReturn(locacoes);
		
		//acao
		service.notificarAtrasos();
		
		//verificacao
		verify(email, times(3)).notificarAtraso(Mockito.any(Usuario.class));
		verify(email).notificarAtraso(usuario);
		verify(email, Mockito.atLeastOnce()).notificarAtraso(usuario3);
		verify(email, never()).notificarAtraso(usuario2);
		verifyNoMoreInteractions(email);
	}
	
	@Test
	public void deveTratarErronoSPC() throws Exception{
		//cenario
		Usuario usuario = UsuarioBuilder.umUsuario().agora();
		List<Filme> filmes = Arrays.asList(FilmeBuilder.umFilme().agora());
		
		when(spc.possuiNegativacao(usuario)).thenThrow(new Exception("Falha catratrófica"));
		
		//verificacao
		exception.expect(LocadoraException.class);
		exception.expectMessage("Problemas com SPC, tente novamente");
		
		//acao
		service.alugarFilme(usuario, filmes);
		
	}
	
	@Test
	public void deveProrrogarUmaLocacao(){
		//cenario
		Locacao locacao = LocacaoBuilder.umLocacao().agora();
		
		//acao
		service.prorrogarLocacao(locacao, 3);
		
		//verificacao
		ArgumentCaptor<Locacao> argCapt = ArgumentCaptor.forClass(Locacao.class);
		Mockito.verify(dao).salvar(argCapt.capture());
		Locacao locacaoRetornada = argCapt.getValue();
		
		error.checkThat(locacaoRetornada.getValor(), is(12.0));
		error.checkThat(locacaoRetornada.getDataLocacao(), MatchersProprios.ehHoje());
		error.checkThat(locacaoRetornada.getDataRetorno(), MatchersProprios.ehHojeComDiferencaDias(3));
	}

	// Utilizamos nesse método a Api Reflections do Java. No caso o método privado foi chamado.

	@Test
	public void deveCalcularValorLocacao() throws Exception {
		// cenario
		List<Filme> filmes = Arrays.asList(
				FilmeBuilder.umFilme().agora(),
				FilmeBuilder.umFilme().agora(),
				FilmeBuilder.umFilme().agora(),
				FilmeBuilder.umFilme().agora());

		// acao
		Class<LocacaoService> classe = LocacaoService.class;
		Method metodo = classe.getDeclaredMethod("calcularValorLocacao", List.class);
		metodo.setAccessible(true);
		Double valor = (Double) metodo.invoke(service, filmes);

		// verificacao          -> valor de cada filme 4, considerando os descontos 13
		Assert.assertThat(valor, CoreMatchers.is(13.0));

	}

}
