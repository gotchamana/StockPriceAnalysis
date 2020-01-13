package stockanalysis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.scene.control.TextInputControl;

import com.jfoenix.validation.base.ValidatorBase;

public class FileExistValidator extends ValidatorBase {
	
	public FileExistValidator() {
		setMessage("No such file");
	}

	@Override
	protected void eval() {
		if (srcControl.get() instanceof TextInputControl) {
			evalTextInputField();
		}
	}

	private void evalTextInputField() {
		TextInputControl textField = (TextInputControl) srcControl.get();
		Path path = Paths.get(textField.getText());
		hasErrors.set(!Files.exists(path));
	}
}
