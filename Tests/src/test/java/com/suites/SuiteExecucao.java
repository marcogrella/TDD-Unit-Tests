package com.suites;


import com.servicos.CalculadoraTest;
import com.servicos.CalculoValorLocacaoTest;
import com.servicos.LocacaoServiceTest;
import com.servicos.LocacaoServiceTest_PowerMock;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	CalculadoraTest.class,
	CalculoValorLocacaoTest.class,
	LocacaoServiceTest.class,
	LocacaoServiceTest_PowerMock.class
})
public class SuiteExecucao {
	//Remova se puder!
}
