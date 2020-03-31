package stockanalysis.control;

import java.util.concurrent.TimeUnit;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static javafx.scene.input.KeyCode.*;
import static org.testfx.assertions.api.Assertions.*;
import static org.testfx.robot.Motion.*;
import stockanalysis.view.StockAnalysisPane;

@EnabledIfSystemProperty(named = "class.test", matches = "Controller|All")
@ExtendWith(ApplicationExtension.class)
public class ControllerTest {
	
	private StockAnalysisPane root;

	@BeforeAll
	public static void init() {
		/* System.setProperty("testfx.robot", "glass");
		System.setProperty("testfx.headless", "true");
		System.setProperty("prism.order", "sw");
		System.setProperty("prism.text", "t2k"); */
	}

	@Start
	private void start(Stage stage) {
		root = new StockAnalysisPane();

		Controller controller = new Controller(stage, root);
		controller.init();

        Scene scene = new Scene(root);
		scene.getStylesheets().add("/style.css");

        stage.setScene(scene);
        stage.setTitle("StockAnalysis");
        stage.show();
	}

	// @Disabled
	@Test
	public void Should_GiveInputRequiredError_When_LeaveBlankToTextFields(FxRobot robot) {
		robot.clickOn(root.analyzeBtn, DEFAULT)
			.sleep(500);

		assertThat(robot.from(root.crashRateInput).lookup(".error-label").queryAs(Label.class)).hasText("Input required");
		assertThat(robot.from(root.filePathInput).lookup(".error-label").queryAs(Label.class)).hasText("Input required");
	}

	// @Disabled
	@Test
	public void Should_GiveDoubleNumberError_When_InputNonNumberToPeakDifferenceAndCrashRateTextField(FxRobot robot) {
		robot.clickOn(root.crashRateInput, DEFAULT)
			.write("abc")
			.clickOn(root.analyzeBtn, DEFAULT)
			.sleep(500);

		assertThat(robot.from(root.crashRateInput).lookup(".error-label").queryAs(Label.class)).hasText("Value must be a rational number");
	}

	// @Disabled
	@Test
	public void Should_GiveFileNotExistError_When_InputFileDoesNotExistTextField(FxRobot robot) {
		robot.clickOn(root.filePathInput, DEFAULT)
			.write("/foo/bar.csv")
			.clickOn(root.analyzeBtn, DEFAULT)
			.sleep(500);

		assertThat(robot.from(root.filePathInput).lookup(".error-label").queryAs(Label.class)).hasText("No such file");
	}

	// @Disabled
	@Test
	public void Should_LeaveTheFilePathTextFieldUnchanged_When_PressSelectFileButtonAndChooseNothing(FxRobot robot) {
		String expected = root.filePathInput.getText();

		robot.clickOn(root.selectFileBtn, DEFAULT)
			.sleep(500)
			.press(ESCAPE);

		assertThat(root.filePathInput).hasText(expected);
	}

	// @Disabled
	@Test
	public void Should_FillInTheCsvFilePathToTheFilePathTextField_When_ChooseACsvFile(FxRobot robot) {
		robot.clickOn(root.selectFileBtn, DEFAULT)
			.sleep(700)
			.type(N).sleep(500)
			.type(ENTER).sleep(500)
			.type(S).sleep(500)
			.type(T).sleep(500)
			.type(O).sleep(500)
			.type(ENTER).sleep(500)
			.type(S).sleep(500)
			.type(ENTER).sleep(500)
			.type(T).sleep(500)
			.type(ENTER).sleep(500)
			.type(R).sleep(500)
			.type(ENTER).sleep(500)
			.type(S).sleep(500)
			.type(ENTER).sleep(500);

		String expected = "/home/shootingstar/NetBeansProjects/StockAnalysis/src/test/resources/stockPrice.csv";
		assertThat(root.filePathInput).hasText(expected);
	}

	// @Disabled
	@Test
	public void Should_EnableOrDisableSaveButton_When_ThereAreAnalysisResultOrNoAnalysisResult(FxRobot robot) {
		root.crashRateInput.setText("10");
		root.filePathInput.setText("/home/shootingstar/NetBeansProjects/StockAnalysis/src/test/resources/stockPrice.csv");

		assertThat(root.saveAnalysisBtn).isDisabled();
		robot.clickOn(root.analyzeBtn, DEFAULT)
			.sleep(10, TimeUnit.SECONDS);
		assertThat(root.saveAnalysisBtn).isEnabled();

		root.crashRateInput.setText("100");
		robot.clickOn(root.analyzeBtn, DEFAULT)
			.sleep(15, TimeUnit.SECONDS);
		assertThat(root.saveAnalysisBtn).isDisabled();
	}

	// @Disabled
	@Test
	public void Should_ChangeTheTotalNumberLabelText_When_AnalyzeData(FxRobot robot) {
		root.crashRateInput.setText("10");
		root.filePathInput.setText("/home/shootingstar/NetBeansProjects/StockAnalysis/src/test/resources/stockPrice.csv");

		robot.clickOn(root.analyzeBtn, DEFAULT)
			.sleep(15, TimeUnit.SECONDS);

		assertThat(root.totalLbl).hasText("Total: 58");
	}
}
