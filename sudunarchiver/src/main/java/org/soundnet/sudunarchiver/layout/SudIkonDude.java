package org.soundnet.sudunarchiver.layout;

	
	import org.kordamp.ikonli.javafx.FontIcon;

	import javafx.scene.paint.Color;
	import javafx.scene.text.Text;

	/**
	 * Handles glyphs from various libraries. 
	 * @author Jamie Macaulay
	 *
	 */
	public class SudIkonDude {
		
	
		/**
		 * Create an Ikonli icon which can be used as graphics for various controls. 
		 * @param iconStr - the icon name to add
		 * @return the icon in a form to add to a control. 
		 */
		public static Text createPamIcon(String iconStr) {
			FontIcon icon2 = new FontIcon(iconStr);
			return icon2;
		}
		
		
		/**
		 * Create an Ikonli icon which can be used as graphics for various controls. 
		 * @param iconStr - the icon name to add
		 * @param color - colour to set the icon to
		 * @param size - the width of the icon in pixels
		 * @return the icon in a form to add to a control. 
		 */
		public static Text createPamIcon(String iconStr, Color color, int size) {
			//Note - the setIconCOlor dows not seem to work so had to use a different way to create the icon using CSS. 
			FontIcon icon2 =new FontIcon(); 
			//do not use above.
			String style = "-fx-icon-code: \"" + iconStr + "\";" +  " -fx-icon-color: " + colourToHex(color)+ ";" + " -fx-icon-size: " + size + ";";
			icon2.setStyle(style);
			return icon2;
		}
		

		
		/**
		 * Create an Ikonli icon which can be used as graphics for various controls. 
		 * @param iconStr - the icon name to add
		 * @param size - the width of the icon in pixels
		 * @return the icon in a form to add to a control. 
		 */
		public static Text createPamIcon(String iconStr, int size) {
//			FontIcon icon2 = (FontIcon) createPamIcon(iconStr);
//			icon2.setIconSize(size);
			
			
			FontIcon icon2 =new FontIcon(); 
			String style = "-fx-icon-code: \"" + iconStr + "\";" + " -fx-icon-size: " + size + ";";
			icon2.setStyle(style);

			return icon2;
		}
		

		/**
		 * Convert a colour object to a hex code
		 * @param color
		 * @return the colour hex code. 
		 */
		public static String colourToHex(Color color){

			int r= (int) (255*color.getRed());
			int g=(int) (255*color.getGreen());
			int b=(int) (255*color.getBlue());

			StringBuilder sb = new StringBuilder("#");

			if (r < 16) sb.append('0');
			sb.append(Integer.toHexString(r));

			if (g < 16) sb.append('0');
			sb.append(Integer.toHexString(g));

			if (b < 16) sb.append('0');
			sb.append(Integer.toHexString(b));

			return sb.toString();
		}



	}



