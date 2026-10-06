package com.vihan.hospital.storage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import net.coobird.thumbnailator.Thumbnails;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.vihan.hospital.common.error.ApiException.BadRequestException;

/**
 * Validates and stores uploaded images (DESIGN.md §12.4): max 5 MB, JPEG / PNG / WebP checked by
 * magic bytes, re-encoded to JPEG (drops EXIF such as GPS location), resized, random file names.
 */
@Service
public class ImageService {

	public static final long MAX_BYTES = 5L * 1024 * 1024;
	private static final int MAX_SOURCE_PIXELS = 40_000_000;  // ~40 MP: refuse decompression bombs
	private static final float JPEG_QUALITY = 0.85f;
	private static final int THUMB_SIZE = 480;

	/** Size limits per use. Images are only ever scaled down, never up. */
	public enum Kind {
		DOCTOR_PHOTO("doctors", 800, 800, false),
		BRANCH_IMAGE("branches", 1600, 1200, false),
		GALLERY("gallery", 1600, 1200, true);

		final String folder;
		final int maxWidth;
		final int maxHeight;
		final boolean thumbnail;

		Kind(String folder, int maxWidth, int maxHeight, boolean thumbnail) {
			this.folder = folder;
			this.maxWidth = maxWidth;
			this.maxHeight = maxHeight;
			this.thumbnail = thumbnail;
		}
	}

	/** Storage keys of the saved image; {@code thumbKey} is null unless the kind makes thumbnails. */
	public record StoredImage(String key, String thumbKey) {
		public List<String> keys() {
			return Arrays.asList(key, thumbKey).stream().filter(Objects::nonNull).toList();
		}
	}

	private final FileStorageService storage;

	public ImageService(FileStorageService storage) {
		this.storage = storage;
	}

	public StoredImage store(MultipartFile file, Kind kind) {
		byte[] bytes = readChecked(file);
		BufferedImage source = decode(bytes);
		try {
			String name = UUID.randomUUID().toString().replace("-", "");
			String key = kind.folder + "/" + name + ".jpg";
			storage.save(key, toJpeg(source, kind.maxWidth, kind.maxHeight));
			String thumbKey = null;
			if (kind.thumbnail) {
				thumbKey = kind.folder + "/" + name + "_thumb.jpg";
				storage.save(thumbKey, toJpeg(source, THUMB_SIZE, THUMB_SIZE));
			}
			return new StoredImage(key, thumbKey);
		}
		catch (IOException e) {
			throw new BadRequestException("INVALID_IMAGE", "The image could not be processed");
		}
	}

	/** Deletes a previously stored image; ignores null and non-upload values such as placeholders. */
	public void deleteQuietly(String key) {
		if (key != null && !key.startsWith("/") && !key.startsWith("http")) {
			storage.delete(key);
		}
	}

	/**
	 * Ties file cleanup to the surrounding DB transaction: after commit the replaced files are
	 * deleted; after rollback the newly written files are deleted instead, so the DB and the
	 * disk never point at different images.
	 */
	public void cleanupOnCompletion(List<String> replacedKeys, List<String> newKeys) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			replacedKeys.forEach(this::deleteQuietly);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				List<String> toDelete = status == STATUS_COMMITTED ? replacedKeys : newKeys;
				toDelete.forEach(ImageService.this::deleteQuietly);
			}
		});
	}

	private static byte[] readChecked(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("FILE_REQUIRED", "Please choose an image file");
		}
		if (file.getSize() > MAX_BYTES) {
			throw new BadRequestException("FILE_TOO_LARGE", "File is larger than 5 MB");
		}
		try {
			byte[] bytes = file.getBytes();
			if (!isJpeg(bytes) && !isPng(bytes) && !isWebp(bytes)) {
				throw new BadRequestException("UNSUPPORTED_IMAGE", "Only JPEG, PNG or WebP images are allowed");
			}
			return bytes;
		}
		catch (IOException e) {
			throw new BadRequestException("INVALID_IMAGE", "The image could not be read");
		}
	}

	private static BufferedImage decode(byte[] bytes) {
		try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (!readers.hasNext()) {
				throw new BadRequestException("UNSUPPORTED_IMAGE", "Only JPEG, PNG or WebP images are allowed");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(in, true, true);
				// Check dimensions from the header before decoding the pixels.
				long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
				if (pixels > MAX_SOURCE_PIXELS) {
					throw new BadRequestException("IMAGE_TOO_LARGE", "Image dimensions are too large");
				}
				return reader.read(0);
			}
			finally {
				reader.dispose();
			}
		}
		catch (IOException | RuntimeException e) {
			if (e instanceof BadRequestException bad) {
				throw bad;
			}
			throw new BadRequestException("INVALID_IMAGE", "The image could not be read");
		}
	}

	private static byte[] toJpeg(BufferedImage source, int maxWidth, int maxHeight) throws IOException {
		BufferedImage rgb = flattenOnWhite(source);   // JPEG has no transparency
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		var builder = Thumbnails.of(rgb);
		if (rgb.getWidth() > maxWidth || rgb.getHeight() > maxHeight) {
			builder.size(maxWidth, maxHeight);           // keeps aspect ratio
		}
		else {
			builder.scale(1.0);
		}
		builder.outputFormat("jpg").outputQuality(JPEG_QUALITY).toOutputStream(out);
		return out.toByteArray();
	}

	private static BufferedImage flattenOnWhite(BufferedImage source) {
		BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = rgb.createGraphics();
		try {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
			g.drawImage(source, 0, 0, null);
		}
		finally {
			g.dispose();
		}
		return rgb;
	}

	static boolean isJpeg(byte[] b) {
		return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
	}

	static boolean isPng(byte[] b) {
		return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
	}

	static boolean isWebp(byte[] b) {
		return b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
				&& b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
	}

}
