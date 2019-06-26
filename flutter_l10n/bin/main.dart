import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:args/args.dart';
import 'package:l10n_generator/flutter_module.dart';
import 'package:path/path.dart';

const String sourceKey = 'source';
const String sourceDefault = './res';

const String outputKey = 'output';
const String outputDefault = './lib/src/generated';

const String watchKey = 'watch';
const bool watchDefault = false;

const String createPathsKey = 'create-paths';
const bool createPathsKeyDefault = true;

Future<void> main(List<String> arguments) async {
  final ArgParser parser = ArgParser()
    ..addOption(
      sourceKey,
      abbr: 's',
      help: 'Specify where to search for the arb files.',
      valueHelp: sourceDefault,
      defaultsTo: sourceDefault,
    )
    ..addOption(
      outputKey,
      abbr: 'o',
      help: 'Specify where to save the generated dart files.',
      valueHelp: outputDefault,
      defaultsTo: outputDefault,
    )
    ..addFlag(
      watchKey,
      abbr: 'w',
      help: 'Whether you want to listen for file changes.\n'
          'NOTE: Keep in mind that the changes are detected when the file is\n'
          'saved after modification. So if you use an IDE make sure to save\n'
          'the arb file so that the dart files are updated.',
      defaultsTo: watchDefault,
    )
    ..addFlag(
      'create-paths',
      abbr: 'c',
      help: 'This will create the folders structure recursevly.',
      defaultsTo: createPathsKeyDefault,
    );

  if (arguments.isNotEmpty && arguments[0] == 'help') {
    stdout.writeln(parser.usage);
    return;
  }

  final ArgResults result = parser.parse(arguments);

  final String source = canonicalize(absolute(result[sourceKey]));
  final String output = canonicalize(absolute(result[outputKey]));
  final bool watch = result[watchKey];
  final bool createPaths = result[createPathsKey];

  final Directory sourceDir = Directory(source);
  final Directory outputDir = Directory(output);

  if (createPaths) {
    if (!sourceDir.existsSync()) {
      sourceDir.createSync(recursive: true);
    }

    if (!outputDir.existsSync()) {
      outputDir.createSync(recursive: true);
    }
  }

  final FlutterModule module = FlutterModule(sourceDir, outputDir);
  module.init();
  module.createFiles();

  if (watch) {
    final Completer<void> completer = Completer<void>();
    final StreamSubscription<String> sub = stdin //
        .transform(utf8.decoder)
        .transform(const LineSplitter())
        .listen((String args) => args == 'x' ? completer.complete() : null);

    module.watch;
    await completer.future;

    sub.cancel();
  }

  module.dispose();
}
