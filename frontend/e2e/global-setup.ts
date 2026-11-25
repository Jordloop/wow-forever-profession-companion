import { execFileSync } from 'node:child_process';
import { resolve } from 'node:path';

const repositoryRoot = resolve(process.cwd(), '..');
const composeProject = `profession-companion-security-${process.pid}`;
const composeArguments = [
  'compose',
  '--project-name',
  composeProject,
  '--env-file',
  '.env.example',
  '-f',
  'compose.security-test.yaml',
];

function runCompose(command: string[]): void {
  execFileSync('docker', [...composeArguments, ...command], {
    cwd: repositoryRoot,
    stdio: 'inherit',
  });
}

export default async function globalSetup(): Promise<() => void> {
  try {
    runCompose(['up', '--detach', '--build', '--wait']);
  } catch (error) {
    runCompose(['down', '--volumes', '--remove-orphans']);
    throw error;
  }

  return () => runCompose(['down', '--volumes', '--remove-orphans']);
}
