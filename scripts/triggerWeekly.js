import {Octokit} from "@octokit/rest";

async function main() {
    const octokit = new Octokit({auth: process.env.GITHUB_TOKEN});
    const now = new Date();

    let currentMonth = (now.getMonth() + 1).toString().padStart(2, "0");
    let dayOfMonth = (now.getDate() + 1).toString().padStart(2, "0");

    await octokit.actions.createWorkflowDispatch({
        owner: "lamba92",
        repo: "leveldb-builds",
        workflow_id: "build.yml",
        ref: "master",
        inputs: {
            "leveldb-ref": "main",
            "debug": "false",
            "version-name": `weekly-${now.getFullYear()}-${currentMonth}-${dayOfMonth}`
        }
    });
}

main().catch(error => {
    console.error("Error executing script:", error);
    process.exit(1);
});
