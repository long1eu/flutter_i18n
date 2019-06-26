import 'dart:convert';
import 'dart:io';

import 'package:l10n_generator/flutter_module.dart';
import 'package:path/path.dart' as path;
import 'package:test/test.dart';

void main() {
  test('get strings files', () async {
    final Directory source = Directory('./test/res/get_strings_files');
    final FlutterModule module = FlutterModule(source);
    module.init();

    expect(module.files.length, 8);

    final File deleteFile = module.files[0];
    deleteFile.deleteSync();
    expect(module.files.length, 7);

    deleteFile
      ..createSync()
      ..writeAsStringSync('{}');

    expect(module.files.length, 8);

    module.dispose();
  });

  test('get strings files from an invalid folder', () {
    Directory source = Directory('./test/res/unexisting_folder');
    expect(() => FlutterModule(source), throwsA(isA<AssertionError>()));

    source = Directory('./test/res/get_strings_files_deleted_folder');
    final FlutterModule module = FlutterModule(source);
    module.init();

    source.deleteSync();

    expect(() => module.files, throwsArgumentError);

    source.createSync();
  });

  test('create file for language', () {
    const String language = 'en';
    final Directory source = Directory('./test/res/create_file_for_language');
    final FlutterModule module = FlutterModule(source);

    final File languageFile = File('${source.path}/strings_$language.arb');
    expect(languageFile.existsSync(), isFalse);

    final File result = module.createFileForLanguage(language);

    expect(path.equals(result.path, languageFile.path), isTrue);
    expect(languageFile.existsSync(), isTrue);
    expect(languageFile.readAsStringSync(), '{}');

    languageFile.deleteSync();
  });

  test('get values', () {
    final Directory source = Directory('./test/res/get_strings_files');
    final FlutterModule module = FlutterModule(source);

    expect(module.values.length, 8);
    expect(module.values.values.every((Map<String, String> it) => it != null && it.isNotEmpty), isTrue);
    expect(module.values.values.every((Map<String, String> it) => it['app_name'] != null && it['app_name'].isEmpty),
        isTrue);
  });

  test('find plural keys', () {
    final Directory source = Directory('./test/res/get_strings_files');
    final FlutterModule module = FlutterModule(source);

    final File pluralsKeysTestList = File('./test/res/plurals/plurals_keys.json');
    final List<String> keys = List<String>.from(jsonDecode(pluralsKeysTestList.readAsStringSync()));

    final PluralsResult result = module.findPluralsKeys(keys);

    expect(result, isNotNull);
    expect(result.pluralsQuantities, isNotNull);
    expect(result.pluralsIds, isNotNull);
    expect(result.pluralsIds.isNotEmpty, result.pluralsQuantities.isNotEmpty);
    expect(result.pluralsIds.isEmpty, isFalse);

    expect(result.pluralsQuantities, containsPair('characterCount', <String>['zero', 'one', 'two', 'other']));
    expect(result.pluralsQuantities, containsPair('carCount_', <String>['few', 'other']));

    expect(result.pluralsIds.length, 6);
    expect(result.pluralsIds.length,
        result.pluralsQuantities.values.fold(0, (int sum, List<String> list) => sum + list.length));
  });

  test('create values method', () {
    final Directory source = Directory('./test/res/get_strings_files');
    final FlutterModule module = FlutterModule(source);

    const String key = 'key';
    const String value = 'value';
    const bool isOverride = true;

    final File dataFile = File('./test/res/create_values_method.txt');
    final List<String> lines = dataFile.readAsLinesSync();

    String result = module.createValuesMethod(key, value);
    expect(result, lines.take(2).join('\n'));

    result = module.createValuesMethod(key, value, isOverride: isOverride);
    expect(result, lines.skip(2).take(3).join('\n'));
  });

  test('create parametrized method', () {
    final Directory source = Directory('./test/res/get_strings_files');
    final FlutterModule module = FlutterModule(source);

    const String key = 'key';
    const String value1 = 'This \$param method.';
    const String value2 = 'This \$param1 method and \$param2.';
    const String value3 = 'value';
    const bool isOverride = true;

    final File dataFile = File('./test/res/create_parametrized_method.txt');
    final List<String> lines = dataFile.readAsLinesSync();

    String result = module.createParametrizedMethod(key, value1);
    expect(result, lines.take(2).join('\n'));

    result = module.createParametrizedMethod(key, value2);
    expect(result, lines.skip(2).take(2).join('\n'));

    result = module.createParametrizedMethod(key, value1, isOverride: isOverride);
    expect(result, lines.skip(4).take(3).join('\n'));

    result = module.createParametrizedMethod(key, value3);
    expect(result, lines.skip(7).take(2).join('\n'));
  });

  test('create plural method', () {
    final Directory source = Directory('./test/res/plurals');
    final FlutterModule module = FlutterModule(source);

    const String key1 = 'characterCount';

    final Map<String, String> data = module.getLanguageData(module.files.first);

    final File dataFile = File('./test/res/create_plural_method.txt');
    final List<String> lines = dataFile.readAsLinesSync();

    final PluralsResult pluralsResult = module.findPluralsKeys(data.keys.toList());

    String result = module.createPluralMethod(key1, pluralsResult.pluralsQuantities[key1], data);
    expect(result, lines.take(17).join('\n'));

    result = module.createPluralMethod(key1, pluralsResult.pluralsQuantities[key1], data);
    expect(result, lines.skip(18).take(17).join('\n'));
  });

  test('create S class', () {
    final Directory source = Directory('./test/res/get_strings_files');
    final FlutterModule module = FlutterModule(source);

    final File englishFile = module.files.firstWhere((File it) => it.path.endsWith('_en.arb'));
    final Map<String, String> englishData = module.getLanguageData(englishFile);

    final String result = module.createSClass(englishData);

    expect(result, isNotNull);
    expect(result, File('./test/res/s_class.txt').readAsStringSync());
  });

  test('create language class', () {
    final Directory source = Directory('./test/res/language_class');
    final FlutterModule module = FlutterModule(source);

    String language = 'id';
    final File englishFile = module.files.firstWhere((File it) => it.path.endsWith('_en.arb'));
    final Map<String, String> englishData = module.getLanguageData(englishFile);

    File languageFile = module.files.firstWhere((File it) => it.path.endsWith('_$language.arb'));
    Map<String, String> languageData = module.getLanguageData(languageFile);

    String result = module.createLanguageClass(language, englishData.keys.toList(), languageData);
    expect(result, File('./test/res/id_class.txt').readAsStringSync());

    language = 'iw';
    languageFile = module.files.firstWhere((File it) => it.path.endsWith('_$language.arb'));
    languageData = module.getLanguageData(languageFile);

    result = module.createLanguageClass(language, englishData.keys.toList(), languageData);
    expect(result, File('./test/res/iw_class.txt').readAsStringSync());
  });

  test('create delegate class', () {
    final Directory source = Directory('./test/res/language_class');
    final FlutterModule module = FlutterModule(source);

    final List<String> languages =
        List<String>.from(jsonDecode(File('./test/res/delegate_languages.json').readAsStringSync()));

    final String result = module.createDelegateClass(languages);
    expect(result, File('./test/res/delegate_class.txt').readAsStringSync());
  });

  test('generate file', () {
    final Directory source = Directory('./test/res/language_class');
    final FlutterModule module = FlutterModule(source);

    expect(module.createGeneratedFile, File('./test/res/generate.txt').readAsStringSync());
  });

  test('create all files', () {
    final Directory source = Directory('./test/res/language_class');
    final FlutterModule module = FlutterModule(source);

    expect(module.output.listSync(), isEmpty);
    module.createFiles();
    expect(module.output.listSync().length, module.files.length + 1);

    module.output.deleteSync(recursive: true);
  });

  // this is not an actual test.
  test(
    'modify files',
    () async {
      final Directory source = Directory('./test/res/language_class');
      final FlutterModule module = FlutterModule(source);
      module.createFiles();
      module.watch;

      final Iterable<MapEntry<String, String>> entries =
          module.files.map((File it) => MapEntry<String, String>(it.path, it.readAsStringSync()));
      final Map<String, String> data = Map<String, String>.fromEntries(entries);

      await Future<void>.delayed(Duration(minutes: 1));

      source.deleteSync(recursive: true);
      source.createSync(recursive: true);
      for (String path in data.keys) {
        File(path).writeAsStringSync(data[path], flush: true);
      }
    },
    timeout: Timeout(Duration(minutes: 1, seconds: 1)),
  );
}
