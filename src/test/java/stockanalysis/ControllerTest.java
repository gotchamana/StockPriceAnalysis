package stockanalysis;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.testfx.assertions.api.Assertions.*;
import static org.testfx.robot.Motion.*;

@ExtendWith(ApplicationExtension.class)
public class ControllerTest {
	
	private StockAnalysisPane root;

	@BeforeAll
	public static void init() {
		System.setProperty("testfx.robot", "glass");
		System.setProperty("testfx.headless", "true");
		System.setProperty("prism.order", "sw");
		System.setProperty("prism.text", "t2k");
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

	@Test
	public void Should_GiveInputRequiredError_When_LeaveBlankToTextFields(FxRobot robot) {
		robot.clickOn(root.analyzeBtn, DEFAULT);

		assertThat(robot.from(root.peakDurationInput).lookup(".error-label").queryAs(Label.class)).hasText("Input required");
		assertThat(robot.from(root.peakDifferenceInput).lookup(".error-label").queryAs(Label.class)).hasText("Input required");
		assertThat(robot.from(root.crashRateInput).lookup(".error-label").queryAs(Label.class)).hasText("Input required");
		assertThat(robot.from(root.filePathInput).lookup(".error-label").queryAs(Label.class)).hasText("Input required");
	}
}
