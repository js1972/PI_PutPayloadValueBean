package custom.sap.module;

public class ModuleContextVarsException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public ModuleContextVarsException() {
	}

	public ModuleContextVarsException(String message) {
		super(message);
	}

	public ModuleContextVarsException(Throwable cause) {
		super(cause);
	}

	public ModuleContextVarsException(String message, Throwable cause) {
		super(message, cause);
	}
}
