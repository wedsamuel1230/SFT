import unittest
import base64
import tempfile
import os

from convert import (
    detect_conversion, 
    resolve_output_path, 
    validate_output_target,
    read_input_to_bytes,
    write_output_from_bytes,
    convert_with_io_handling,
)


class TestConvertDetection(unittest.TestCase):
    def test_csv_to_xlsx_detection(self):
        self.assertEqual(detect_conversion("data.csv", None, "xlsx"), "csv-to-xlsx")
        self.assertEqual(detect_conversion("data.csv", "output", "xlsx"), "csv-to-xlsx")

    def test_xlsx_to_csv_detection(self):
        self.assertEqual(detect_conversion("book.xlsx", None, "csv"), "xlsx-to-csv")

    def test_image_conversion_detection(self):
        self.assertEqual(detect_conversion("image.png", "image.jpg", None), "image-convert")

    def test_output_target_conflict_raises(self):
        with self.assertRaises(ValueError):
            validate_output_target("output.pdf", "xlsx")

    def test_resolve_output_path_appends_extension(self):
        self.assertEqual(resolve_output_path("data.csv", "output", "xlsx"), "output.xlsx")


class TestIOHandling(unittest.TestCase):
    def setUp(self):
        # Create a temporary CSV file for testing
        self.test_csv_content = "name,age\nJohn,30\nJane,25"
        self.test_csv_bytes = self.test_csv_content.encode('utf-8')
        self.temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False)
        self.temp_file.write(self.test_csv_content)
        self.temp_file.close()
        self.temp_path = self.temp_file.name

    def tearDown(self):
        if os.path.exists(self.temp_path):
            os.unlink(self.temp_path)

    def test_read_input_file_path(self):
        data = read_input_to_bytes("file_path", self.temp_path)
        # Normalize line endings for cross-platform compatibility
        normalized_data = data.replace(b'\r\n', b'\n')
        self.assertEqual(normalized_data, self.test_csv_bytes)

    def test_read_input_base64(self):
        b64_data = base64.b64encode(self.test_csv_bytes).decode('ascii')
        data = read_input_to_bytes("base64", b64_data)
        self.assertEqual(data, self.test_csv_bytes)

    def test_read_input_invalid_file(self):
        with self.assertRaises(ValueError):
            read_input_to_bytes("file_path", "nonexistent.csv")

    def test_read_input_invalid_base64(self):
        with self.assertRaises(ValueError):
            read_input_to_bytes("base64", "invalid-base64!")

    def test_read_input_invalid_type(self):
        with self.assertRaises(ValueError):
            read_input_to_bytes("invalid_type", "value")

    def test_write_output_file_path(self):
        with tempfile.NamedTemporaryFile(delete=False) as tmp:
            tmp_path = tmp.name
        
        try:
            result = write_output_from_bytes("file_path", tmp_path, self.test_csv_bytes)
            self.assertEqual(result, tmp_path)
            with open(tmp_path, "rb") as f:
                self.assertEqual(f.read(), self.test_csv_bytes)
        finally:
            if os.path.exists(tmp_path):
                os.unlink(tmp_path)

    def test_write_output_base64(self):
        expected_b64 = base64.b64encode(self.test_csv_bytes).decode('ascii')
        result = write_output_from_bytes("base64", "", self.test_csv_bytes)
        self.assertEqual(result, expected_b64)

    def test_write_output_invalid_type(self):
        with self.assertRaises(ValueError):
            write_output_from_bytes("invalid_type", "value", self.test_csv_bytes)

    def test_convert_with_io_file_to_file(self):
        with tempfile.NamedTemporaryFile(suffix='.xlsx', delete=False) as tmp:
            output_path = tmp.name
        
        try:
            result = convert_with_io_handling(
                input_type="file_path",
                input_value=self.temp_path,
                output_type="file_path", 
                output_value=output_path,
                target_format="xlsx",
            )
            self.assertEqual(result, output_path)
            self.assertTrue(os.path.exists(output_path))
        finally:
            if os.path.exists(output_path):
                os.unlink(output_path)

    def test_convert_with_io_base64_to_base64(self):
        b64_input = base64.b64encode(self.test_csv_bytes).decode('ascii')
        result = convert_with_io_handling(
            input_type="base64",
            input_value=b64_input,
            output_type="base64",
            output_value="",
            target_format="xlsx",
            input_format="csv",
        )
        # Should return base64 encoded XLSX data
        self.assertIsInstance(result, str)
        # Decode and check it's not the original data
        decoded = base64.b64decode(result)
        self.assertNotEqual(decoded, self.test_csv_bytes)


if __name__ == "__main__":
    unittest.main()
