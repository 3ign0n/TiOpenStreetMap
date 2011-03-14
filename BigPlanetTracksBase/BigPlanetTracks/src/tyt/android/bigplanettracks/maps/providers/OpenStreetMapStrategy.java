package tyt.android.bigplanettracks.maps.providers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class OpenStreetMapStrategy extends MapStrategy {

	private List<Layer> layers = new ArrayList<Layer>();

	public OpenStreetMapStrategy() {
		layers.add(new Layer() {

			private  String SERVER = "http://b.tile.openstreetmap.org/";
			
			@Override
			public String getDescription() {
				return null;
			}

			@Override
			public int getId() {
				return 0;
			}

			@Override
			public String getURLPattern() {
				return  SERVER+"{2}/{0}/{1}.png";
			}

		});

	}

	@Override
	public String getURL(int x, int y, int z, int layout) {
		Layer layer = layers.get(layout);
		return MessageFormat.format(layer.getURLPattern(),
				String.valueOf(x), String.valueOf(y), String.valueOf(17-z));
	}

	@Override
	public String getDescription() {
		return "OpenStreetMap";
	}

}
