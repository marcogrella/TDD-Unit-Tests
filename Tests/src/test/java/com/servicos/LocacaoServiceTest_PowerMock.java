package com.servicos;


import com.builders.FilmeBuilder;
import com.builders.UsuarioBuilder;
import com.daos.LocacaoDAO;
import com.entidades.Filme;
import com.entidades.Locacao;
import com.entidades.Usuario;
import com.matchers.MatchersProprios;
import com.utils.DataUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(PowerMockRunner.class)
@PrepareForTest({LocacaoService.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class LocacaoServiceTest_PowerMock {

	@InjectMocks
	private LocacaoService service = new LocacaoService();;
	
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

		service = PowerMockito.spy(service);
	}
	
	@Test
	public void deveAlugarFilme() throws Exception {
		//cenario
		Usuario usuario = UsuarioBuilder.umUsuario().agora();
		List<Filme> filmes = Arrays.asList(FilmeBuilder.umFilme().comValor(5.0).agora());
		
		PowerMockito.whenNew(Date.class).withNoArguments().thenReturn(DataUtils.obterData(5, 11, 2021));

		// O PowerMock também trabalha com métodos estáticos. Exemplo a Calendar. No exemplo acima utilizamos a Date;
		// Calendar calendar = Calendar.getInstance();
		// calendar.set(Calendar.DAY_OF_MONTH, 5);
		// calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
		// calendar.set(Calendar.YEAR, 2021);

		// PowerMockito.mockStatic(Calendar.class);
		// PowerMockito.when(Calendar.getInstance()).thenReturn(calendar);

		//acao
		Locacao locacao = service.alugarFilme(usuario, filmes);
			
		//verificacao
		error.checkThat(locacao.getValor(), is(equalTo(5.0)));
		//error.checkThat(locacao.getDataLocacao(), MatchersProprios.ehHoje());
		// error.checkThat(locacao.getDataRetorno(), MatchersProprios.ehHojeComDiferencaDias(1));
		error.checkThat(DataUtils.isMesmaData(locacao.getDataLocacao(), DataUtils.obterData(5, 11, 2021)), is(true));
	}
	

	@Test
	public void deveDevolverNaSegundaAoAlugarNoSabado() throws Exception{
		//cenario
		Usuario usuario = UsuarioBuilder.umUsuario().agora();
		List<Filme> filmes = Arrays.asList(FilmeBuilder.umFilme().agora());
		
		 PowerMockito.whenNew(Date.class).withNoArguments().thenReturn(DataUtils.obterData(6, 11, 2021));

		// O PowerMock também trabalha com métodos estáticos. Exemplo a Calendar. No exemplo acima utilizamos a Date;
		// Calendar calendar = Calendar.getInstance();
		// calendar.set(Calendar.DAY_OF_MONTH, 6);
		// calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
		// calendar.set(Calendar.YEAR, 2021);

		// PowerMockito.mockStatic(Calendar.class);
		// PowerMockito.when(Calendar.getInstance()).thenReturn(calendar);


		//acao
		Locacao retorno = service.alugarFilme(usuario, filmes);
		
		// verificacao
		assertThat(retorno.getDataRetorno(), MatchersProprios.caiNumaSegunda());
		 // PowerMockito.verifyNew(Date.class, Mockito.times(2)).withNoArguments(); // opcional
		// PowerMockito.verifyStatic(Calendar.class, Mockito.times(2));
		Calendar.getInstance();

	}
	

	@Test
	public void deveAlugarFilme_SemCalcularValor() throws Exception {
		// cenario
		Usuario usuario = UsuarioBuilder.umUsuario().agora();
		List<Filme> filmes = Arrays.asList(FilmeBuilder.umFilme().agora());

		// Neste exemplo vamos chamar o calculo privado. Ele na verdade nem é executado e sim mockado.
		PowerMockito.doReturn(1.0).when(service, "calcularValorLocacao", filmes); // nome do método tem que ser String

		// acao
		Locacao locacao = service.alugarFilme(usuario, filmes);

		// verificacao
		Assert.assertThat(locacao.getValor(), CoreMatchers.is(1.0));
		PowerMockito.verifyPrivate(service).invoke("calcularValorLocacao", filmes);


	}

	@Test
	public void deveCalcularValorLocacao() throws Exception {
		// cenario
		List<Filme> filmes = Arrays.asList(
				FilmeBuilder.umFilme().agora(),
				FilmeBuilder.umFilme().agora(),
				FilmeBuilder.umFilme().agora(),
				FilmeBuilder.umFilme().agora());

		// acao -> utiliza um método privado.
		Double valor = (Double) Whitebox.invokeMethod(service, "calcularValorLocacao", filmes);

		// verificacao          -> valor de cada filme 4, considerando os descontos 13
		Assert.assertThat(valor, CoreMatchers.is(13.0));

	}

}
