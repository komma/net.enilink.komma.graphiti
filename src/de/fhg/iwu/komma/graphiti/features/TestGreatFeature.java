package de.fhg.iwu.komma.graphiti.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;

import com.google.inject.Inject;

public class TestGreatFeature extends AbstractCustomFeature {
	@Inject
	public TestGreatFeature(IFeatureProvider fp) {
		super(fp);
	}

	/**
	 * This function contains the code doing the cool stuff of this feature
	 */
	@Override
	public void execute(ICustomContext context) {
		System.out.print("\nWOW! What a great feature!\n\n");
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		// cool stuff could be done here to allow execution only for specific
		// items
		return true;// can always be executed...
	}

	@Override
	public String getName() {
		return "great Feature";
	}

	@Override
	public String getDescription() {
		return "You will be amazed by this feature, because it is so great!";
	}

}
