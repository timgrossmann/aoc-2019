import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public class Solution {

	static class InputRequiredException extends RuntimeException {
	}

	public static class Computer {
		private final Deque<Long> stdIn = new ArrayDeque<>();
		private final Deque<Long> stdOut = new ArrayDeque<>();

		private final long[] program;

		private final Map<Long, Long> memory;

		private Optional<Long> next;

		private long base = 0L;

		private final Map<Integer, BiFunction<Long, Long, Long>> numericOperations = Map.of(
			1, (a, b) -> a + b,
			2, (a, b) -> a * b
		);

		private final Map<Integer, Function<Long, Boolean>> conditions = Map.of(
			5, (a) -> a != 0L,
			6, (a) -> a == 0L
		);

		private final Map<Integer, BiFunction<Long, Long, Boolean>> comparisons = Map.of(
			7, (a, b) -> a < b,
			8, (a, b) -> a.equals(b)
		);

		public static Computer loadProgram(List<Long> instructions) {
			return new Computer(instructions.stream().mapToLong(Long::longValue).toArray());
		}

		private Computer(long[] program) {
			this.program = program;
			this.memory = Collections.synchronizedMap(new HashMap<>());
			this.next = Optional.of(0L);
		}

		public Computer run() {

			return this.run(true);
		}

		public Computer run(boolean resetAfterHalt) {

			while (next.isPresent()) {
				try {
					next = next.flatMap(this::executeInstruction);
				} catch (InputRequiredException e) {
					break;
				}
			}

			if (!next.isPresent() && resetAfterHalt) {
				this.reset();
			}
			return this;
		}

		public Computer reset() {

			this.memory.clear();
			this.next = Optional.of(0L);
			this.base = 0L;
			return this;
		}

		private Optional<Long> readStdIn() {

			return Optional.ofNullable(stdIn.poll());
		}

		private void writeStdOut(Long in) {

			stdOut.push(in);
		}

		public Computer pipe(Long in) {

			stdIn.push(in);
			return this;
		}

		public List<Long> drain() {

			var output = new ArrayList<Long>(this.stdOut.size());
			this.stdOut.descendingIterator().forEachRemaining(output::add);
			this.stdOut.clear();
			return output;
		}

		public Optional<Long> head() {

			return Optional.ofNullable(this.stdOut.poll());
		}

		public boolean expectsInput() {
			return next.isPresent();
		}

		private Optional<Long> executeInstruction(long ptr) {

			var opcodeAndModes = Math.toIntExact(load(ptr++, 0));
			var opcode = opcodeAndModes % 100;
			int[] modes = extractModes(opcodeAndModes);
			return switch (opcode) {
				case 1, 2 -> {
					var p1 = load(load(ptr++, 0), modes[0]);
					var p2 = load(load(ptr++, 0), modes[1]);
					store(load(ptr++, 0), modes[2], numericOperations.get(opcode).apply(p1, p2));
					yield Optional.of(ptr);
				}
				case 3 -> {
					var v = readStdIn().orElseThrow(InputRequiredException::new);
					store(load(ptr++, 0), modes[0], v);
					yield Optional.of(ptr);
				}
				case 4 -> {
					writeStdOut(load(load(ptr++, 0), modes[0]));
					yield Optional.of(ptr);
				}
				case 5, 6 -> {
					var p1 = load(load(ptr++, 0), modes[0]);
					var p2 = load(load(ptr++, 0), modes[1]);
					yield Optional.of(conditions.get(opcode).apply(p1) ? p2 : ptr);
				}
				case 7, 8 -> {
					var p1 = load(load(ptr++, 0), modes[0]);
					var p2 = load(load(ptr++, 0), modes[1]);
					store(load(ptr++, 0), modes[2], comparisons.get(opcode).apply(p1, p2) ? 1L : 0L);
					yield Optional.of(ptr);
				}
				case 9 -> {
					var l = load(load(ptr++, 0), modes[0]);
					base += l;
					yield Optional.of(ptr);
				}
				case 99 -> Optional.empty();
				default -> throw new IllegalArgumentException(String.format("Invalid opcode %d", opcode));
			};
		}

		private static int[] extractModes(Integer of) {
			return IntStream.rangeClosed(3, 5)
				.map(i -> of % (int) Math.pow(10, i) / (int) Math.pow(10, i - 1))
				.toArray();
		}

		private long resolveAddress(long address, int mode) {

			return mode == 2 ? base + address : address;
		}

		private Long load(long p, int mode) {

			if (mode == 1) {
				return p;
			}

			var address = resolveAddress(p, mode);
			return this.memory.computeIfAbsent(address, i -> i > program.length ? 0L : program[Math.toIntExact(i)]);
		}

		private void store(long p, int mode, Long v) {

			var address = resolveAddress(p, mode);
			this.memory.put(address, v);
		}
	}

	public static void main(String... a) throws IOException {

		final var EMPTY = 0L;
		final var WALL = 1L;
		final var BLOCK = 2L;
		final var PADDLE = 3L;

		var instructions = Files.readAllLines(Path.of("input.txt")).stream().flatMap(s -> Arrays.stream(s.split(",")))
			.map(String::trim)
			.map(Long::parseLong)
			.collect(toList());

		// tag::starOne[]
		var computer = Computer.loadProgram(instructions);
		List<Long> output = computer.run().drain();
		int cnt = 0;
		for (int i = 0; i < output.size(); ++i) {
			if (i % 3 == 2 && output.get(i) == BLOCK) {
				++cnt;
			}
		}
		System.out.println(String.format("Star one %d", cnt));
		// end::starOne[]

		// tag::starTwo[]
		instructions.set(0, 2L); // <.>
		var paddlePattern = List.of(EMPTY, PADDLE, EMPTY); // <.>
		for (int i = 0; i < instructions.size(); i += 2) {
			if (instructions.subList(i, i + 3).equals(paddlePattern)) { // <.>
				var paddleAt = i + 1;
				var j = paddleAt;
				while (instructions.get(--j) != WALL) {
					instructions.set(j, PADDLE);
				}
				j = paddleAt;
				while (instructions.get(++j) != WALL) {
					instructions.set(j, PADDLE);
				}
				break;
			}
		}
		computer = Computer.loadProgram(instructions); // <.>
		while (computer.run(false).expectsInput()) {
			computer.pipe(0L); // <.>
		}
		computer.head()
			.ifPresent(d -> System.out.println(String.format("Star two %d", d)));
		// end::starTwo[]
	}
}
