package net.coderbot.iris.gl.program;

import com.google.common.collect.ImmutableList;
import net.coderbot.iris.gl.image.ImageBinding;
import net.coderbot.iris.gl.image.ImageHolder;
import net.coderbot.iris.gl.image.ImageLimits;
import net.coderbot.iris.gl.texture.InternalTextureFormat;
import org.lwjgl.opengl.GL20C;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

public class ProgramImages {
	private final ImmutableList<ImageBinding> imageBindings;
	private List<GlUniform1iCall> initializer;

	private ProgramImages(ImmutableList<ImageBinding> imageBindings, List<GlUniform1iCall> initializer) {
		this.imageBindings = imageBindings;
		this.initializer = initializer;
	}

	public void update() {
		if (initializer != null) {
			for (GlUniform1iCall call : initializer) {
				GL20C.glUniform1i(call.getLocation(), call.getValue());
			}

			initializer = null;
		}

		for (ImageBinding imageBinding : imageBindings) {
			imageBinding.update();
		}
	}

	public static Builder builder(int program) {
		return new Builder(program);
	}

	public static final class Builder implements ImageHolder {
		private final int program;
		private final ImmutableList.Builder<ImageBinding> images;
		private final List<GlUniform1iCall> calls;
		private int nextImageUnit;
		private final int maxImageUnits;

		private Builder(int program) {
			this.program = program;
			this.images = ImmutableList.builder();
			this.calls = new ArrayList<>();
			this.nextImageUnit = 0;
			this.maxImageUnits = ImageLimits.get().getMaxImageUnits();
		}

		@Override
		public void addTextureImage(IntSupplier textureID, InternalTextureFormat internalFormat, String name) {
			int location = GL20C.glGetUniformLocation(program, name);

			if (location == -1) {
				return;
			}

			if (nextImageUnit >= maxImageUnits) {
				if (maxImageUnits == 0) {
					throw new IllegalStateException("Image units are not supported on this platform, but a shader" +
							" program attempted to reference " + name + ".");
				} else {
					throw new IllegalStateException("No more available texture units while activating image " + name + "." +
							" Only " + maxImageUnits + " image units are available.");
				}
			}

			images.add(new ImageBinding(nextImageUnit, internalFormat.getGlFormat(), textureID));
			calls.add(new GlUniform1iCall(location, nextImageUnit));

			nextImageUnit += 1;
		}

		public ProgramImages build() {
			return new ProgramImages(images.build(), calls);
		}
	}
}
