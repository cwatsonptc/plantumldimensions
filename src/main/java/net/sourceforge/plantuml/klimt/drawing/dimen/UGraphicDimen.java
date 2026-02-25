package net.sourceforge.plantuml.klimt.drawing.dimen;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import net.sourceforge.plantuml.klimt.ClipContainer;
import net.sourceforge.plantuml.klimt.UPath;
import net.sourceforge.plantuml.klimt.UShape;
import net.sourceforge.plantuml.klimt.color.ColorMapper;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.klimt.color.HColorMiddle;
import net.sourceforge.plantuml.klimt.color.HColorSimple;
import net.sourceforge.plantuml.klimt.color.HColors;
import net.sourceforge.plantuml.klimt.drawing.AbstractCommonUGraphic;
import net.sourceforge.plantuml.klimt.geom.USegment;
import net.sourceforge.plantuml.klimt.geom.USegmentType;
import net.sourceforge.plantuml.klimt.geom.XDimension2D;
import net.sourceforge.plantuml.klimt.geom.XPoint2D;
import net.sourceforge.plantuml.klimt.shape.DotPath;
import net.sourceforge.plantuml.klimt.shape.UCenteredCharacter;
import net.sourceforge.plantuml.klimt.shape.UComment;
import net.sourceforge.plantuml.klimt.shape.UEllipse;
import net.sourceforge.plantuml.klimt.shape.UEmpty;
import net.sourceforge.plantuml.klimt.shape.ULine;
import net.sourceforge.plantuml.klimt.shape.UPolygon;
import net.sourceforge.plantuml.klimt.shape.URectangle;
import net.sourceforge.plantuml.klimt.shape.UText;
import net.sourceforge.plantuml.klimt.drawing.debug.StringBounderDebug;

public class UGraphicDimen extends AbstractCommonUGraphic implements ClipContainer {

	private final List<String> output;
	private final double scaleFactor;
	private final XDimension2D dim;
	private final String svgLinkTarget;
	private final String hoverPathColorRGB;
	private final long seed;
	private final String preserveAspectRatio;
	private final Deque<Comment> comments;

	String toJSONPoint(XPoint2D pt) {
		if (pt == null)
			return "null";

		return "{ \"X\": \"" + String.format(Locale.US, "%.4f", pt.getX()) + "\", "
				+ "\"Y\": \"" + String.format(Locale.US, "%.4f", pt.getY()) + "\" }";
	}

	private class Comment {
		private final String comment;
		XPoint2D pt1 = null;
		XPoint2D pt2 = null;

		public Comment(String comment) {
			this.comment = comment;
		}

		public void setPt1(XPoint2D pt1) {
			if (this.pt1 == null) {
				this.pt1 = pt1;
			} else {
				double x = Math.min(this.pt1.getX(), pt1.getX());
				double y = Math.min(this.pt1.getY(), pt1.getY());
				this.pt1 = new XPoint2D(x, y);
			}
		}

		public void setPt2(XPoint2D pt2) {
			if (this.pt2 == null) {
				this.pt2 = pt2;
			} else {
				double x = Math.max(this.pt2.getX(), pt2.getX());
				double y = Math.max(this.pt2.getY(), pt2.getY());
				this.pt2 = new XPoint2D(x, y);
			}
		}

		@Override
		public String toString() {
			return comment + (pt1 == null || pt2 == null ? "" : " dim=" + pointd(pt1.getX(), pt1.getY()) + " - " + pointd(pt2.getX(), pt2.getY()));
		}

		public String toJSON() {
			StringBuilder result = new StringBuilder();
			result.append("{");
			result.append("\"entity\": \"" + comment + "\"");
			if (pt1 != null)
				result.append(", \"pt1\": " + toJSONPoint(pt1));
			if (pt2 != null)
				result.append(", \"pt2\": " + toJSONPoint(pt2));
			result.append("}");
			return result.toString();
		}
	}

	@Override
	protected AbstractCommonUGraphic copyUGraphic() {
		final UGraphicDimen result = new UGraphicDimen(this, output, scaleFactor, dim, svgLinkTarget, hoverPathColorRGB,
				seed, preserveAspectRatio, comments);
		return result;
	}

	private UGraphicDimen(UGraphicDimen other, List<String> output, double scaleFactor, XDimension2D dim,
			String svgLinkTarget, String hoverPathColorRGB, long seed, String preserveAspectRatio,
			Deque<Comment> comments) {
		super(other.getStringBounder());
		basicCopy(other);
		this.output = output;
		this.scaleFactor = scaleFactor;
		this.dim = dim;
		this.svgLinkTarget = svgLinkTarget;
		this.hoverPathColorRGB = hoverPathColorRGB;
		this.seed = seed;
		this.preserveAspectRatio = preserveAspectRatio;
		this.comments = comments;
	}

	public UGraphicDimen(double scaleFactor, XDimension2D dim, String svgLinkTarget, String hoverPathColorRGB,
			long seed, String preserveAspectRatio) {
		super(new StringBounderDebug());
		basicCopy(HColors.WHITE, ColorMapper.IDENTITY);
		this.output = new ArrayList<>();
		this.comments = new ArrayDeque<>();
		this.scaleFactor = scaleFactor;
		this.dim = dim;
		this.svgLinkTarget = svgLinkTarget;
		this.hoverPathColorRGB = hoverPathColorRGB;
		this.seed = seed;
		this.preserveAspectRatio = preserveAspectRatio;
	}

	public void draw(UShape shape) {
		if (shape instanceof ULine) {
			outLine((ULine) shape);
		} else if (shape instanceof URectangle) {
			outRectangle((URectangle) shape);
		} else if (shape instanceof UText) {
			outText((UText) shape);
		} else if (shape instanceof UPolygon) {
			outPolygon((UPolygon) shape);
		} else if (shape instanceof UEllipse) {
			outEllipse((UEllipse) shape);
		} else if (shape instanceof UEmpty) {
			outEmpty((UEmpty) shape);
		} else if (shape instanceof UPath) {
			outPath((UPath) shape);
		} else if (shape instanceof UComment) {
			outComment((UComment) shape);
		} else if (shape instanceof DotPath) {
			outPath(((DotPath) shape).toUPath());
		} else if (shape instanceof UCenteredCharacter) {
			outCenteredCharacter(((UCenteredCharacter) shape));
		} else {
			System.err.println("UGraphicDimen " + shape.getClass().getSimpleName());
			output.add("UGraphicDimen " + shape.getClass().getSimpleName() + " " + new Date());
		}
	}

	private void outCenteredCharacter(UCenteredCharacter shape) {
		output.add("CENTERED_CHAR:");
		output.add("  char: " + shape.getChar());
		output.add("  position: " + pointd(getTranslateX(), getTranslateY()));
		output.add("  font: " + shape.getFont().toString());
		output.add("  color: " + colorToString(getParam().getColor()));
		output.add("");

	}

	private void outComment(UComment shape) {
		comments.push(new Comment(shape.getComment()));
	}

	private void outPath(UPath shape) {
		output.add("PATH:");
		for (USegment seg : shape) {
			final USegmentType type = seg.getSegmentType();
			final double coord[] = seg.getCoord();
			output.add("   - type: " + type);
			if (type == USegmentType.SEG_ARCTO) {
				output.add("     radius: " + pointd(coord[0], coord[1]));
				output.add("     angle: " + coord[2]);
				output.add("     largeArcFlag: " + (coord[3] != 0));
				output.add("     sweepFlag: " + (coord[4] != 0));
				output.add("     dest: " + pointd(coord[5], coord[6]));
			} else
				for (int i = 0; i < type.getNbPoints(); i++) {
					final String key = "     pt" + (i + 1) + ": ";
					output.add(key + pointd(coord[2 * i], coord[2 * i + 1]));
				}
		}

		output.add("  stroke: " + getParam().getStroke());
		output.add("  shadow: " + (int) shape.getDeltaShadow());
		output.add("  color: " + colorToString(getParam().getColor()));
		output.add("  backcolor: " + colorToString(getParam().getBackcolor()));
		output.add("");

	}

	private void outPolygon(UPolygon shape) {
		output.add("POLYGON:");
		output.add("  points:");
		for (XPoint2D pt : shape.getPoints()) {
			final double xp = getTranslateX() + pt.getX();
			final double yp = getTranslateY() + pt.getY();
			output.add("   - " + pointd(xp, yp));
		}
		output.add("  stroke: " + getParam().getStroke());
		output.add("  shadow: " + (int) shape.getDeltaShadow());
		output.add("  color: " + colorToString(getParam().getColor()));
		output.add("  backcolor: " + colorToString(getParam().getBackcolor()));
		output.add("");

	}

	private void outText(UText shape) {
		System.err.println("UText");
		if (comments.isEmpty() == false) {
			comments.peek().setPt1(new XPoint2D(getTranslateX(), getTranslateY()));
		}
		output.add("TEXT:");
		output.add("  text: " + shape.getText());
		output.add("  position: " + pointd(getTranslateX(), getTranslateY()));
		output.add("  orientation: " + shape.getOrientation());
		output.add("  font: " + shape.getFontConfiguration().toStringDebug());
		output.add("  color: " + colorToString(shape.getFontConfiguration().getColor()));
		output.add("  extendedColor: " + colorToString(shape.getFontConfiguration().getExtendedColor()));
		output.add("");
	}

	private void outEmpty(UEmpty shape) {
		output.add("EMPTY:");
		output.add("  pt1: " + pointd(getTranslateX(), getTranslateY()));
		output.add("  pt2: " + pointd(getTranslateX() + shape.getWidth(), getTranslateY() + shape.getHeight()));
		output.add("");

	}

	private void outEllipse(UEllipse shape) {
		output.add("ELLIPSE:");
		output.add("  pt1: " + pointd(getTranslateX(), getTranslateY()));
		output.add("  pt2: " + pointd(getTranslateX() + shape.getWidth(), getTranslateY() + shape.getHeight()));
		output.add("  start: " + shape.getStart());
		output.add("  extend: " + shape.getExtend());
		output.add("  stroke: " + getParam().getStroke());
		output.add("  shadow: " + (int) shape.getDeltaShadow());
		output.add("  color: " + colorToString(getParam().getColor()));
		output.add("  backcolor: " + colorToString(getParam().getBackcolor()));
		output.add("");

	}

	private void outRectangle(URectangle shape) {
		System.err.println("1 URectangle " + getTranslateX() + "x" + getTranslateY());
		System.err.println("2 URectangle " + shape.getWidth() + "x" + shape.getHeight());
		System.err.println("3 URectangle " + (getTranslateX() + shape.getWidth()) + " x " + (getTranslateY() + shape.getHeight()));
		if (comments.isEmpty() == false) {
			comments.peek().setPt1(new XPoint2D(getTranslateX(), getTranslateY()));
			comments.peek().setPt2(new XPoint2D(getTranslateX() + shape.getWidth(), getTranslateY() + shape.getHeight()));
		}
		output.add("RECTANGLE:");
		output.add("  pt1: " + pointd(getTranslateX(), getTranslateY()));
		output.add("  pt2: " + pointd(getTranslateX() + shape.getWidth(), getTranslateY() + shape.getHeight()));
		output.add("  xCorner: " + (int) shape.getRx());
		output.add("  yCorner: " + (int) shape.getRy());
		output.add("  stroke: " + getParam().getStroke());
		output.add("  shadow: " + (int) shape.getDeltaShadow());
		output.add("  color: " + colorToString(getParam().getColor()));
		output.add("  backcolor: " + colorToString(getParam().getBackcolor()));
		output.add("");

	}

	private void outLine(ULine shape) {
		output.add("LINE:");
		output.add("  pt1: " + pointd(getTranslateX(), getTranslateY()));
		output.add("  pt2: " + pointd(getTranslateX() + shape.getDX(), getTranslateY() + shape.getDY()));
		output.add("  stroke: " + getParam().getStroke());
		output.add("  shadow: " + (int) shape.getDeltaShadow());
		output.add("  color: " + colorToString(getParam().getColor()));
		output.add("");

	}

	private String pointd(double x, double y) {
		return String.format(Locale.US, "[ %.4f ; %.4f ]", x, y);
	}

	private String colorToString(HColor color) {
		if (color == null || color.isTransparent())
			return "NULL_COLOR";

		if (color instanceof HColorSimple) {
			final HColorSimple simple = (HColorSimple) color;
			final Color internal = simple.getAwtColor();

			return Integer.toHexString(internal.getRGB());
		}
		if (color instanceof HColorMiddle) {
			final HColorMiddle middle = (HColorMiddle) color;
			return "middle(" + colorToString(middle.getColor1()) + " & " + colorToString(middle.getColor1()) + " )";
		}
		System.err.println("Error colorToString " + color.getClass().getSimpleName());
		return color.getClass().getSimpleName() + " " + new Date();
	}

	@Override
	public void writeToStream(OutputStream os, String metadata, int dpi) throws IOException {
		print(os, "{");
		print(os, "\"dimension\": " + toJSONPoint(new XPoint2D(dim.getWidth(), dim.getHeight())));
		print(os, ",\"entities\": " + toJSONComments());
		print(os, "}");
		os.flush();
	}

	private String toJSONComments() {
		if (comments.isEmpty())
			return "[]";

		final StringBuilder result = new StringBuilder();
		result.append("[");
		boolean first = true;
		for (Comment comment : comments) {
			if (first == false)
				result.append(", ");
			first = false;
			result.append(comment.toJSON());
		}
		result.append("]");
		return result.toString();
	}

	private void print(OutputStream os, String out) throws UnsupportedEncodingException, IOException {
		os.write(out.getBytes(UTF_8));
		os.write("\n".getBytes(UTF_8));
	}

}
