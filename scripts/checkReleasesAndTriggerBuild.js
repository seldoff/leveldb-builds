import {Octokit} from "@octokit/rest";
import {SemVer} from "semver";

function normalizeVersion(version) {
    const parts = version.split('.');
    while (parts.length < 3) {
        parts.push('0'); // Add missing minor/patch as "0"
    }
    return {
        original: version,
        semver: new SemVer(parts.join('.'), true)
    };
}

async function main() {
    const octokit = new Octokit({auth: process.env.GITHUB_TOKEN});

    const googleReleases = await octokit.paginate("GET /repos/google/leveldb/releases");
    const googleTags = googleReleases.map(release => release.tag_name);

    const lambaReleases = await octokit.paginate("GET /repos/lamba92/leveldb-builds/releases");
    const lambaTags = new Set(lambaReleases.map(release => release.tag_name));
    console.log("Already built releases:", lambaTags);

    const minimumVersion = new SemVer("1.21.0", true);

    const newReleases = googleTags
        .filter(tag => !lambaTags.has(tag))
        .map(tag => normalizeVersion(tag))
        .filter(normalized => normalized.semver.compare(minimumVersion) >= 0)
        .map(normalized => normalized.original);

    if (newReleases.length === 0) {
        console.log(`No new releases to trigger after version ${minimumVersion.version}`);
        return;
    }

    console.log("New releases to trigger:", newReleases);

    for (const release of newReleases) {
        await octokit.actions.createWorkflowDispatch({
            owner: "lamba92",
            repo: "leveldb-builds",
            workflow_id: "build.yml",
            ref: "master",
            inputs: {
                "leveldb-version": release,
                "enable-windows": "true",
                "debug": "false"
            }
        });
    }
}

main().catch(error => {
    console.error("Error executing script:", error);
    process.exit(1);
});
